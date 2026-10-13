package com.feixu.myustc

import android.content.Context
import java.time.LocalDate

object TodayCourseProvider {
    fun calculateWeekNumber(context: Context, date: LocalDate): Int? {
        val semesterStart = CourseDataManager.getSemesterStart(context) ?: return null
        return CourseDataManager.calculateWeekNumber(semesterStart, date).takeIf { it >= 1 }
    }

    fun getCoursesForDate(context: Context, date: LocalDate): List<CourseData> {
        val week = calculateWeekNumber(context, date) ?: return emptyList()
        val dayOfWeek = date.dayOfWeek.value
        return CourseDataManager.mergeCourses(context)
            .filter { course -> course.dayOfWeek == dayOfWeek && course.isInWeek(week) }
            .sortedWith(compareBy<CourseData> { it.startTime }.thenBy { it.name }.thenBy { it.location })
    }
}
