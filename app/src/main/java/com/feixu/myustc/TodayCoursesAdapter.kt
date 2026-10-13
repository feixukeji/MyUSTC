package com.feixu.myustc

import android.content.Intent
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class TodayCoursesAdapter(private val coursesList: List<CourseData>) :
    RecyclerView.Adapter<TodayCoursesAdapter.CourseViewHolder>() {
    //今日课程适配器

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorBar: View = itemView.findViewById(R.id.color_bar)
        val courseName: TextView = itemView.findViewById(R.id.course_name)
        val courseStatus: TextView = itemView.findViewById(R.id.course_status)
        val courseTeacher: TextView = itemView.findViewById(R.id.course_teacher)
        val courseTime: TextView = itemView.findViewById(R.id.course_time)
        val courseLocation: TextView = itemView.findViewById(R.id.course_classroom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.today_course_item, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        try {
            val course = coursesList[position]
            val context = holder.itemView.context

            // 计算课程状态
            val now = LocalTime.now()
            val isEnded = now.isAfter(course.endTime)

            // 设置灰色
            val grayColor = "#9E9E9E".toColorInt()

            // 获取主题颜色
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val textColorPrimary = if (typedValue.resourceId != 0) {
                context.resources.getColorStateList(typedValue.resourceId, context.theme)
            } else {
                ColorStateList.valueOf(typedValue.data)
            }

            context.theme.resolveAttribute(android.R.attr.colorControlNormal, typedValue, true)
            val colorControlNormal = if (typedValue.resourceId != 0) {
                context.resources.getColorStateList(typedValue.resourceId, context.theme)
            } else {
                ColorStateList.valueOf(typedValue.data)
            }

            // 设置颜色条
            holder.colorBar.setBackgroundColor(if (isEnded) grayColor else course.color)

            // 课程名称
            holder.courseName.text = course.name
            if (isEnded) {
                holder.courseName.setTextColor(grayColor)
            } else {
                holder.courseName.setTextColor(textColorPrimary)
            }

            // 老师
            holder.courseTeacher.text = TodayCourseDisplayFormatter.formatTeacherLine(course.teacher)
            if (isEnded) {
                holder.courseTeacher.setTextColor(grayColor)
            } else {
                holder.courseTeacher.setTextColor(colorControlNormal)
            }

            // 时间
            holder.courseTime.text = TodayCourseDisplayFormatter.formatTimeLine(course)
            if (isEnded) {
                holder.courseTime.setTextColor(grayColor)
            } else {
                holder.courseTime.setTextColor(colorControlNormal)
            }

            // 教室
            holder.courseLocation.visibility = View.VISIBLE
            holder.courseLocation.text = TodayCourseDisplayFormatter.formatLocationLine(course.location)
            if (isEnded) {
                holder.courseLocation.setTextColor(grayColor)
            } else {
                holder.courseLocation.setTextColor(colorControlNormal)
            }

            // 状态
            val statusText = when {
                now.isBefore(course.startTime) -> {
                    val minutesUntilStart = ChronoUnit.MINUTES.between(now, course.startTime)
                    if (minutesUntilStart <= 20) {
                        "${minutesUntilStart}分钟后开始"
                    } else {
                        "未开始"
                    }
                }

                isEnded -> {
                    "已结束"
                }

                else -> {
                    val minutesUntilEnd = ChronoUnit.MINUTES.between(now, course.endTime)
                    "${minutesUntilEnd}分钟后结束"
                }
            }
            holder.courseStatus.visibility = View.VISIBLE
            holder.courseStatus.text = statusText

            val statusColor = when {
                now.isBefore(course.startTime) -> {
                    val minutesUntilStart = ChronoUnit.MINUTES.between(now, course.startTime)
                    if (minutesUntilStart <= 20) {
                        "#FF9800".toColorInt()
                    } else {
                        grayColor
                    }
                }

                isEnded -> {
                    grayColor
                }

                else -> {
                    "#4CAF50".toColorInt()
                }
            }
            holder.courseStatus.setTextColor(statusColor)

            // 点击事件：跳转到课程表页面
            holder.itemView.setOnClickListener {
                val intent = Intent(context, CourseTableActivity::class.java)
                context.startActivity(intent)
            }

        } catch (_: Exception) {
        }
    }

    override fun getItemCount(): Int = coursesList.size
}
