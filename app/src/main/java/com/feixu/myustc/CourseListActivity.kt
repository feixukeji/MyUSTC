package com.feixu.myustc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CourseListActivity : AppCompatActivity() {
    //自定义课程页面
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_course_list)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        // 根据主题设置Toolbar样式
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)

        //加载课程
        val courses = CoursesHelper.readCourses(this)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = CoursesAdapter(courses)
        checkEmpty(courses)

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "other") //加载背景图片
    }

    fun checkEmpty(courses: MutableList<Course>) {
        //检查自定义课程列表是否为空
        if (courses.isEmpty()) {
            recyclerView?.visibility = View.GONE
            findViewById<TextView>(R.id.text_view).visibility = View.VISIBLE
        } else {
            recyclerView?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.text_view).visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    fun addCourse(view: View) {
        //添加课程
        val editCourseView = LayoutInflater.from(this).inflate(R.layout.edit_course_view, null)
        val courseNameView: EditText = editCourseView.findViewById(R.id.name)
        val courseTeacherView: EditText = editCourseView.findViewById(R.id.teacher)
        val courseWeekView: EditText = editCourseView.findViewById(R.id.week)
        val courseDayView: Spinner = editCourseView.findViewById(R.id.day)
        val courseStartUnitView: Spinner = editCourseView.findViewById(R.id.startUnit)
        val courseEndUnitView: Spinner = editCourseView.findViewById(R.id.endUnit)
        val courseClassroomView: EditText = editCourseView.findViewById(R.id.classroom)
        val dialog = ThemeHelper.createDialogBuilder(this).setView(editCourseView)
            .setPositiveButton("确定") { _, _ -> }
            .setNegativeButton("取消") { _, _ -> }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val courseStartUnit = courseStartUnitView.selectedItemPosition + 1
            val courseEndUnit = courseEndUnitView.selectedItemPosition + 1
            if (courseStartUnit > courseEndUnit) {
                showInvalidCourseUnitDialog()
                return@setOnClickListener
            }

            val courseName = courseNameView.text.toString()
            val courseTeacher = TeacherFormatter.format(courseTeacherView.text.toString())
            val courseWeek = courseWeekView.text.toString()
            val courseDay = courseDayView.selectedItemPosition + 1
            val courseClassroom = courseClassroomView.text.toString()
            val course = Course(courseName, courseTeacher, courseWeek, courseDay, courseStartUnit, courseEndUnit, courseClassroom)

            (recyclerView?.adapter as CoursesAdapter).addCourse(course)
            val courses = CoursesHelper.readCourses(this)
            courses.add(course)
            CoursesHelper.saveCourses(this, courses)

            checkEmpty(courses)
            dialog.dismiss()
        }
    }

    private fun showInvalidCourseUnitDialog() {
        ThemeHelper.createDialogBuilder(this)
            .setMessage("开始节次不能大于结束节次")
            .setPositiveButton("确定") { _, _ -> }
            .show()
    }
}
