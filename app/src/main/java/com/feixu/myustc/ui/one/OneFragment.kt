package com.feixu.myustc.ui.one

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.view.get
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.feixu.myustc.CourseTableActivity
import com.feixu.myustc.Funcs
import com.feixu.myustc.ExamArrangeActivity
import com.feixu.myustc.ExamHelper
import com.feixu.myustc.ExamsAdapter
import com.feixu.myustc.Func
import com.feixu.myustc.FuncsAdapter
import com.feixu.myustc.MainActivity
import com.feixu.myustc.R
import com.feixu.myustc.TaskListActivity
import com.feixu.myustc.TasksAdapter
import com.feixu.myustc.TasksHelper
import com.feixu.myustc.TodayCourseProvider
import com.feixu.myustc.TodayCoursesAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OneFragment : Fragment() {
    //首页

    private var rootView: View? = null

    private fun nonScrollableGridLayoutManager(spanCount: Int): GridLayoutManager {
        return object : GridLayoutManager(context, spanCount) {
            override fun canScrollVertically(): Boolean = false
        }
    }

    private fun nonScrollableLinearLayoutManager(): LinearLayoutManager {
        return object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_one, container, false)
        rootView = root

        //设置下拉刷新
        val refreshLayout: SwipeRefreshLayout = root.findViewById(R.id.refreshLayout)
        val homeScrollView: NestedScrollView = root.findViewById(R.id.home_scroll_view)
        homeScrollView.isNestedScrollingEnabled = false
        homeScrollView.overScrollMode = View.OVER_SCROLL_NEVER
        refreshLayout.overScrollMode = View.OVER_SCROLL_NEVER
        refreshLayout.setOnChildScrollUpCallback { _, _ ->
            homeScrollView.canScrollVertically(-1)
        }
        refreshLayout.setOnRefreshListener {
            loadCommonUse()
            refresh()
            refreshLayout.isRefreshing = false
        }

        loadCommonUse() //加载常用功能

        //全部功能点击监听
        root.findViewById<CardView>(R.id.func_card).setOnClickListener {
            (activity as MainActivity).viewPager?.currentItem = 1
            (activity as MainActivity).navView?.menu?.get(1)?.isChecked = true
        }

        //考试信息点击监听
        root.findViewById<CardView>(R.id.exam_card).setOnClickListener {
            startActivity(Intent(context, ExamArrangeActivity::class.java))
        }

        //今日课程点击监听
        root.findViewById<CardView>(R.id.course_card).setOnClickListener {
            startActivity(Intent(context, CourseTableActivity::class.java))
        }

        //任务清单点击监听
        root.findViewById<CardView>(R.id.task_card).setOnClickListener {
            startActivity(Intent(context, TaskListActivity::class.java))
        }

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun loadPunch() {
        //加载每日签到
        val root = rootView ?: return
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val punchRecord = requireContext().getSharedPreferences("Punch", MODE_PRIVATE)
        val punchCard = root.findViewById<CardView>(R.id.punch_card)
        if (punchRecord.getBoolean("Enable", true)) {
            punchCard.visibility = View.VISIBLE
            if (punchRecord.getString("LastPunch", "") == current) showFortune()
            else { //若今日未签到
                root.findViewById<TextView>(R.id.welcome).text = "欢迎回来"
                root.findViewById<TextView>(R.id.fortune).visibility = View.GONE
                root.findViewById<LinearLayout>(R.id.good_linearLayout).visibility = View.GONE
                root.findViewById<LinearLayout>(R.id.bad_linearLayout).visibility = View.GONE
                root.findViewById<TextView>(R.id.good_detail).visibility = View.VISIBLE
                root.findViewById<TextView>(R.id.bad_detail).visibility = View.VISIBLE
                val punchButton = root.findViewById<Button>(R.id.punch)
                punchButton.visibility = View.VISIBLE
                punchButton.setOnClickListener {
                    punchRecord.edit { putString("Fortune", FortuneData.fortunes.random()) } //随机运势
                    val fortuneDetail = FortuneData.things.shuffled() //随机宜忌
                    punchRecord.edit { putString("Good", fortuneDetail[0].brief) }
                    punchRecord.edit { putString("GoodDetail", fortuneDetail[0].good) }
                    punchRecord.edit { putString("Bad", fortuneDetail[1].brief) }
                    punchRecord.edit { putString("BadDetail", fortuneDetail[1].bad) }
                    punchRecord.edit { putInt("PunchTimes", punchRecord.getInt("PunchTimes", 0) + 1) }
                    punchRecord.edit { putString("LastPunch", current) }
                    showFortune()
                }
                root.findViewById<TextView>(R.id.punch_times).text = "我的科大·已签到" + punchRecord.getInt("PunchTimes", 0) + "天" //签到天数
            }
        } else punchCard.visibility = View.GONE
    }

    private fun refresh() {
        //刷新每日签到、最近考试信息、今日课程、最近任务清单
        loadPunch()
        loadExam()
        loadCourses()
        loadTask()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    @SuppressLint("SetTextI18n")
    private fun showFortune() {
        //显示今日运势
        val root = rootView ?: return
        val punchRecord = requireContext().getSharedPreferences("Punch", MODE_PRIVATE)
        val fortune = punchRecord.getString("Fortune", "")

        root.findViewById<TextView>(R.id.welcome).text = "今日运势"
        val fortuneText: TextView = root.findViewById(R.id.fortune)
        fortuneText.text = "§ $fortune §"
        fortuneText.visibility = View.VISIBLE

        val goodText: TextView = root.findViewById(R.id.good)
        val goodDetailText: TextView = root.findViewById(R.id.good_detail)
        val badText: TextView = root.findViewById(R.id.bad)
        val badDetailText: TextView = root.findViewById(R.id.bad_detail)
        val goodLinearLayout: LinearLayout = root.findViewById(R.id.good_linearLayout)
        val badLinearLayout: LinearLayout = root.findViewById(R.id.bad_linearLayout)
        when (fortune) {
            "大吉" -> {
                goodText.text = "诸事皆宜"
                goodDetailText.visibility = View.GONE
                goodLinearLayout.visibility = View.VISIBLE
            }

            "大寄" -> {
                badText.text = "诸事皆寄"
                badDetailText.visibility = View.GONE
                badLinearLayout.visibility = View.VISIBLE
            }

            else -> {
                goodText.text = "宜：" + punchRecord.getString("Good", "")
                goodDetailText.text = punchRecord.getString("GoodDetail", "")
                badText.text = "忌：" + punchRecord.getString("Bad", "")
                badDetailText.text = punchRecord.getString("BadDetail", "")
                goodLinearLayout.visibility = View.VISIBLE
                badLinearLayout.visibility = View.VISIBLE
            }
        }

        root.findViewById<Button>(R.id.punch).visibility = View.GONE

        root.findViewById<TextView>(R.id.punch_times).text = "我的科大·已签到" + punchRecord.getInt("PunchTimes", 0) + "天"
    }

    private fun loadCommonUse() {
        //加载常用功能
        val root = rootView ?: return
        val commonUse = requireContext().getSharedPreferences("CommonUse", MODE_PRIVATE)
        val allFuncsCnt: MutableList<Pair<Func, Int>> = mutableListOf()
        val settingPersonalize = requireContext().getSharedPreferences("Personalize", MODE_PRIVATE)
        val defaultCommonUse = if (!settingPersonalize.getBoolean("GraduateStudent", false)) setOf(
            "StudySystem", "CourseTable", "ExamArrange", "GradeSheet", "BlackboardLearn", "PhysicalExperiment", "CourseResource", "UstcEmail"
        ) else setOf(
            "GraduateStudentPlatform", "CourseList", "CourseAssignment", "ScoreQuery", "ClassroomQuery", "Library", "UstcEmail", "SchoolBus"
        ) //默认常用功能列表
        Funcs(requireContext()).allFuncs.forEach { item ->
            var cnt = commonUse.getInt(item.name, -1)
            if (cnt == -1 && item.name in defaultCommonUse) cnt = 0
            allFuncsCnt.add(Pair(item, cnt))
        }
        allFuncsCnt.sortByDescending { item -> item.second } //按使用次数排序

        val commonFuncs: MutableList<Func> = mutableListOf()
        allFuncsCnt.subList(0, 8).forEach { item -> commonFuncs.add(item.first) }

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(commonFuncs)
    }

    private fun loadExam() {
        //加载最近考试信息
        val root = rootView ?: return
        val examCard = root.findViewById<CardView>(R.id.exam_card)
        examCard.visibility = View.GONE
        val settingPersonalize = requireContext().getSharedPreferences("Personalize", MODE_PRIVATE)
        if (settingPersonalize.getBoolean("GraduateStudent", false)) return
        try {
            val exams = ExamHelper.getUpcomingExams(requireContext(), limit = 4)
            ExamHelper.scheduleReminders(requireContext(), exams)
            if (exams.isNotEmpty()) {
                val recyclerView: RecyclerView = root.findViewById(R.id.exam_recycler_view)
                recyclerView.layoutManager = nonScrollableLinearLayoutManager()
                recyclerView.adapter = ExamsAdapter(exams)
                examCard.visibility = View.VISIBLE
            }
        } catch (_: Exception) {
        }
    }

    private fun loadCourses() {
        //加载今日课程
        val root = rootView ?: return
        try {
            val today = java.time.LocalDate.now()
            val courseCard: CardView = root.findViewById(R.id.course_card)
            courseCard.visibility = View.GONE

            val todayCourses = TodayCourseProvider.getCoursesForDate(requireContext(), today)

            if (todayCourses.isNotEmpty()) {
                val recyclerView: RecyclerView = root.findViewById(R.id.course_recycler_view)
                recyclerView.layoutManager = nonScrollableLinearLayoutManager()
                recyclerView.adapter = TodayCoursesAdapter(todayCourses)
                courseCard.visibility = View.VISIBLE
            }
        } catch (_: Exception) {
        }
    }

    private fun loadTask() {
        //加载最近任务清单
        val root = rootView ?: return
        val tasks = TasksHelper.readTasks(requireContext())
        if (tasks.isNotEmpty()) {
            val recyclerView: RecyclerView = root.findViewById(R.id.task_recycler_view)
            recyclerView.layoutManager = nonScrollableLinearLayoutManager()
            recyclerView.adapter = TasksAdapter(tasks, 5)
            root.findViewById<CardView>(R.id.task_card).visibility = View.VISIBLE
        } else {
            root.findViewById<CardView>(R.id.task_card).visibility = View.GONE
        }
    }
}
