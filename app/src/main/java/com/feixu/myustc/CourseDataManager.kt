package com.feixu.myustc

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 课程表数据管理器
 * 负责合并教务系统课程和自定义课程数据
 */
object CourseDataManager {

    /**
     * 获取学期开始日期
     */
    fun getSemesterStart(context: Context): LocalDate? {
        val prefStartDateStr = context.getSharedPreferences("Semester", Context.MODE_PRIVATE)
            .getString("StartDate", "")
            ?.trim()
            ?: ""
        if (prefStartDateStr.isNotEmpty()) {
            return try {
                val normalized = if (prefStartDateStr.length >= 10) prefStartDateStr.substring(0, 10) else prefStartDateStr
                LocalDate.parse(normalized)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    fun calculateWeekNumber(semesterStart: LocalDate, date: LocalDate): Int {
        val alignedStart = alignWeekStart(semesterStart)
        val alignedDate = alignWeekStart(date)
        return (ChronoUnit.WEEKS.between(alignedStart, alignedDate) + 1).toInt()
    }

    fun calculateWeekStart(semesterStart: LocalDate, weekNumber: Int): LocalDate {
        return alignWeekStart(semesterStart).plusWeeks((weekNumber - 1).toLong())
    }

    private fun alignWeekStart(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * 合并教务系统课程和自定义课程
     */
    fun mergeCourses(context: Context): List<CourseData> {
        val courses = mutableListOf<CourseData>()

        val schoolCourses = SchoolCourseParser.parseSchoolCourses(context)
        for (course in schoolCourses) {
            val courseData = CourseData.fromSchoolCourse(course) ?: continue
            courses.add(courseData)
        }

        // 添加自定义课程
        val customCourses = CoursesHelper.readCourses(context)
        for (custom in customCourses) {
            courses.add(CourseData.fromCourse(custom))
        }

        return CourseData.assignColors(courses)
    }
}
