package com.feixu.myustc

import android.content.Context

object CoursesHelper {
    //自定义课程助手

    fun readCourses(mContext: Context): MutableList<Course> { //读自定义课程
        val courses: MutableList<Course> = mutableListOf()
        val data = FileHelper.read(mContext, "CustomCourse.fx")
        val coursesData = data.split('\\').filter { it.isNotEmpty() } //不同任务之间用“\“隔开
        coursesData.forEach { item ->
            val courseData = item.split('`') //一个任务的不同信息之间用“`“隔开
            courses.add(Course(courseData[0], TeacherFormatter.format(courseData[1]), courseData[2], courseData[3].toInt(), courseData[4].toInt(), courseData[5].toInt(), courseData[6]))
        }
        return courses
    }

    fun saveCourses(mContext: Context, courses: List<Course>) { //写自定义课程
        val data = StringBuilder()
        courses.forEach { course ->
            data.append(course.name.replace('\\', ' ').replace('`', ' ')).append('`')
                .append(TeacherFormatter.format(course.teacher).replace('\\', ' ').replace('`', ' ')).append('`')
                .append(course.week.replace('\\', ' ').replace('`', ' ')).append('`')
                .append(course.day.toString()).append('`')
                .append(course.startUnit.toString()).append('`')
                .append(course.endUnit.toString()).append('`')
                .append(course.classroom.replace('\\', ' ').replace('`', ' ')).append('\\')
        }
        FileHelper.save(mContext, "CustomCourse.fx", if (data.isEmpty()) "" else data.substring(0, data.length - 1))

        // 自定义课程更新后立即刷新小组件
        TodayCourseWidget.refreshAllWidgets(mContext)
    }
}
