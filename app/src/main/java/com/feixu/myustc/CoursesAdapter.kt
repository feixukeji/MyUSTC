package com.feixu.myustc

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView


class CoursesAdapter(private val coursesList: MutableList<Course>) :
    RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {
    //课程适配器

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseNameView: TextView = itemView.findViewById(R.id.name)
        val courseTimeView: TextView = itemView.findViewById(R.id.time)
        val courseView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.course_item, parent, false)
        return CourseViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = coursesList[position]

        holder.courseNameView.text = course.name //课程名称
        holder.courseTimeView.text = holder.courseView.context.resources.getStringArray(R.array.day_of_week)[course.day - 1] + " 第" + course.startUnit.toString() + "节至第" + course.endUnit.toString() + "节"

        holder.courseView.setOnClickListener { //查看或编辑课程
            try {
                val editCourseView = LayoutInflater.from(holder.courseView.context).inflate(R.layout.edit_course_view, null)
                val courseNameView: EditText = editCourseView.findViewById(R.id.name)
                val courseTeacherView: EditText = editCourseView.findViewById(R.id.teacher)
                val courseWeekView: EditText = editCourseView.findViewById(R.id.week)
                val courseDayView: Spinner = editCourseView.findViewById(R.id.day)
                val courseStartUnitView: Spinner = editCourseView.findViewById(R.id.startUnit)
                val courseEndUnitView: Spinner = editCourseView.findViewById(R.id.endUnit)
                val courseClassroomView: EditText = editCourseView.findViewById(R.id.classroom)
                editCourseView.findViewById<TextView>(R.id.text).text = "课程详情"
                courseNameView.setText(coursesList[position].name)
                courseTeacherView.setText(coursesList[position].teacher)
                courseWeekView.setText(coursesList[position].week)
                courseDayView.setSelection(coursesList[position].day - 1)
                courseStartUnitView.setSelection(coursesList[position].startUnit - 1)
                courseEndUnitView.setSelection(coursesList[position].endUnit - 1)
                courseClassroomView.setText(coursesList[position].classroom)
                val dialog = ThemeHelper.createDialogBuilder(holder.courseView.context).setView(editCourseView)
                    .setPositiveButton("确定") { _, _ -> }
                    .setNegativeButton("取消") { _, _ -> }
                    .setNeutralButton("删除") { _, _ ->
                        coursesList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, coursesList.size - position)
                        CoursesHelper.saveCourses(holder.courseView.context, coursesList)
                        (holder.courseView.context as CourseListActivity).checkEmpty(coursesList) //检查自定义课程列表是否为空
                    }
                    .show()

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val courseStartUnit = courseStartUnitView.selectedItemPosition + 1
                    val courseEndUnit = courseEndUnitView.selectedItemPosition + 1
                    if (courseStartUnit > courseEndUnit) {
                        showInvalidCourseUnitDialog(holder.courseView)
                        return@setOnClickListener
                    }

                    coursesList[position].name = courseNameView.text.toString()
                    coursesList[position].teacher = TeacherFormatter.format(courseTeacherView.text.toString())
                    coursesList[position].week = courseWeekView.text.toString()
                    coursesList[position].day = courseDayView.selectedItemPosition + 1
                    coursesList[position].startUnit = courseStartUnit
                    coursesList[position].endUnit = courseEndUnit
                    coursesList[position].classroom = courseClassroomView.text.toString()
                    notifyItemChanged(position)
                    CoursesHelper.saveCourses(holder.courseView.context, coursesList)
                    dialog.dismiss()
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun getItemCount(): Int {
        return coursesList.size
    }

    fun addCourse(course: Course) {
        //告知适配器添加课程
        coursesList.add(course)
        notifyItemInserted(coursesList.lastIndex)
    }

    private fun showInvalidCourseUnitDialog(view: View) {
        ThemeHelper.createDialogBuilder(view.context)
            .setMessage("开始节次不能大于结束节次")
            .setPositiveButton("确定") { _, _ -> }
            .show()
    }
}
