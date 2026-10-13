package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CourseTableActivity : AppCompatActivity() {
    // 原生课程表页面

    private lateinit var courseWeekView: CourseWeekView
    private lateinit var weekInfoText: TextView
    private lateinit var dateInfoText: TextView
    private lateinit var emptyHint: TextView
    private lateinit var embeddedSync: StudySystemEmbeddedSync

    private var currentWeek: Int = 1
    private var displayWeek: Int = 1
    private var showAllInTable: Boolean = false
    private var semesterStart: LocalDate? = null
    private var allCourses: List<CourseData> = emptyList()
    private var hasStartedInitialAutoSync = false
    private val minDisplayWeek = -52
    private val maxDisplayWeek = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
        val graduateStudent = settingPersonalize.getBoolean("GraduateStudent", false)

        if (graduateStudent) {
            finish()
            val intent = Intent(this, WebActivity::class.java)
            intent.putExtra("text", "课程表")
            intent.putExtra("name", "CourseList")
            startActivity(intent)
            return
        }

        setContentView(R.layout.activity_course_table)
        initLayout()
    }

    private fun initLayout() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)
        ThemeHelper.applyStatusBarStyle(this)

        courseWeekView = findViewById(R.id.course_week_view)
        val courseScrollView: View = findViewById(R.id.course_scroll_view)
        courseScrollView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val viewportHeight = view.height - view.paddingTop - view.paddingBottom
            courseWeekView.setViewportHeight(viewportHeight)
        }
        weekInfoText = findViewById(R.id.week_info)
        dateInfoText = findViewById(R.id.date_info)
        emptyHint = findViewById(R.id.empty_hint)
        embeddedSync = StudySystemEmbeddedSync.courseTable(
            this,
            findViewById(R.id.sync_web_view),
            findViewById(R.id.progress_bar)
        )

        findViewById<Button>(R.id.btn_prev_week).setOnClickListener {
            val minWeek = if (semesterStart != null) minDisplayWeek else 1
            if (displayWeek > minWeek) {
                displayWeek--
                loadCoursesForWeek()
            }
        }

        findViewById<Button>(R.id.btn_next_week).setOnClickListener {
            if (displayWeek < maxDisplayWeek) {
                displayWeek++
                loadCoursesForWeek()
            }
        }

        findViewById<Button>(R.id.btn_this_week).setOnClickListener {
            displayWeek = if (semesterStart != null) currentWeek else 1
            if (semesterStart != null) {
                displayWeek = displayWeek.coerceIn(minDisplayWeek, maxDisplayWeek)
            }
            loadCoursesForWeek()
        }

        findViewById<Button>(R.id.btn_all_courses).setOnClickListener {
            showAllInTable = !showAllInTable
            updateAllToggleButton()
            loadCoursesForWeek()
        }

        courseWeekView.onCourseClickListener = { courses ->
            showCourseDetail(courses)
        }

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "other")
    }

    override fun onResume() {
        super.onResume()
        loadAllCourses()
        if (!hasStartedInitialAutoSync) {
            hasStartedInitialAutoSync = true
            val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
            if (settingPersonalize.getBoolean("AutoSync", true)) {
                syncCourseTable(showDialog = false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.course_table_menu, menu)
        ThemeHelper.applyMenuIconStyle(this, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sync_course -> syncCourseTable(showDialog = true)
            R.id.add_course -> startActivity(Intent(this, CourseListActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadAllCourses(keepDisplayWeek: Boolean = false) {
        val previousDisplayWeek = displayWeek

        allCourses = CourseDataManager.mergeCourses(this)
        semesterStart = CourseDataManager.getSemesterStart(this)

        val today = LocalDate.now()
        currentWeek = semesterStart?.let { CourseDataManager.calculateWeekNumber(it, today) } ?: -1
        if (semesterStart != null) {
            displayWeek = if (keepDisplayWeek) previousDisplayWeek else currentWeek
            displayWeek = displayWeek.coerceIn(minDisplayWeek, maxDisplayWeek)
        } else {
            displayWeek = if (keepDisplayWeek) previousDisplayWeek.coerceIn(1, maxDisplayWeek) else 1
        }

        updateAllToggleButton()
        loadCoursesForWeek()
    }

    @SuppressLint("SetTextI18n")
    private fun updateAllToggleButton() {
        val allButton = findViewById<Button>(R.id.btn_all_courses)
        val prevButton = findViewById<Button>(R.id.btn_prev_week)
        val thisButton = findViewById<Button>(R.id.btn_this_week)
        val nextButton = findViewById<Button>(R.id.btn_next_week)

        if (showAllInTable) {
            allButton.text = "单"
            allButton.contentDescription = "切换到单周课程"
        } else {
            allButton.text = "全"
            allButton.contentDescription = "切换到全部课程"
        }

        val enableWeekSwitch = !showAllInTable
        prevButton.isEnabled = enableWeekSwitch
        thisButton.isEnabled = enableWeekSwitch
        nextButton.isEnabled = enableWeekSwitch

        val alpha = if (enableWeekSwitch) 1f else 0.45f
        prevButton.alpha = alpha
        thisButton.alpha = alpha
        nextButton.alpha = alpha
    }

    @SuppressLint("SetTextI18n")
    private fun loadCoursesForWeek() {
        val shownCourses = if (showAllInTable) {
            allCourses
        } else {
            allCourses.filter { it.isInWeek(displayWeek) }
        }
        val today = LocalDate.now()
        val isCurrentWeek = displayWeek == currentWeek
        val isBeforeSemesterStart = semesterStart?.let { today.isBefore(it) } == true
        val weekStart = CourseDataManager.calculateWeekStart(semesterStart ?: today, displayWeek)

        if (shownCourses.isEmpty()) {
            courseWeekView.visibility = View.GONE
            emptyHint.visibility = View.VISIBLE
            emptyHint.text = when {
                showAllInTable -> if (allCourses.isNotEmpty()) "暂无可显示的课程" else "暂无课程数据\n点击右上角云朵按钮同步课程表"
                isCurrentWeek && isBeforeSemesterStart -> "假期中"
                isCurrentWeek -> "本周没有课程"
                displayWeek < 1 -> "假期中"
                allCourses.isNotEmpty() -> "本周没有课程"
                else -> "暂无课程数据\n点击右上角云朵按钮同步课程表"
            }
        } else {
            courseWeekView.visibility = View.VISIBLE
            emptyHint.visibility = View.GONE

            courseWeekView.setWeekStart(weekStart)
            courseWeekView.setCourses(shownCourses, showAllInTable)
        }

        weekInfoText.text = when {
            showAllInTable -> "我的科大·全部课程"
            displayWeek < 1 -> "我的科大·假期中"
            else -> "我的科大·第${displayWeek}周"
        }

        val weekEnd = weekStart.plusDays(6)
        val formatter = DateTimeFormatter.ofPattern("MM-dd")
        dateInfoText.text = if (showAllInTable) {
            "按星期与节次汇总"
        } else {
            "${weekStart.format(formatter)} ~ ${weekEnd.format(formatter)}"
        }
    }

    private fun syncCourseTable(showDialog: Boolean) {
        val previousSemesterStart = semesterStart
        embeddedSync.sync(
            showDialog = showDialog,
            onSuccess = {
                val semesterStartUnchanged = previousSemesterStart == CourseDataManager.getSemesterStart(this)
                loadAllCourses(keepDisplayWeek = semesterStartUnchanged)
            }
        )
    }

    private fun showCourseDetail(courses: List<CourseData>) {
        if (courses.isEmpty()) return

        val sortedCourses = courses.sortedWith(
            compareBy<CourseData> { it.dayOfWeek }
                .thenBy { it.startUnit }
                .thenBy { it.endUnit }
                .thenBy { it.name }
        )

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val message = StringBuilder()

        sortedCourses.forEachIndexed { index, course ->
            if (sortedCourses.size > 1) {
                message.append("第${index + 1}门课\n")
            }

            message.append("名称：${course.name}\n")
            if (course.teacher.isNotEmpty()) {
                message.append("教师：${course.teacher}\n")
            }
            message.append("周次：${course.simplifiedWeeksWithSuffixText()}\n")
            message.append("星期：${getDayName(course.dayOfWeek)}\n")
            message.append("节次：${course.unitRangeText()}节\n")
            message.append("时间：${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}\n")
            if (course.location.isNotEmpty()) {
                message.append("教室：${course.location}\n")
            }

            if (index != sortedCourses.lastIndex) {
                message.append("\n")
            }
        }

        ThemeHelper.createDialogBuilder(this)
            .setTitle(if (sortedCourses.size > 1) "叠课详情" else "课程信息")
            .setMessage(message.toString())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            7 -> "星期日"
            else -> ""
        }
    }
}
