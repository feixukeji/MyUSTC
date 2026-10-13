package com.feixu.myustc

import java.time.format.DateTimeFormatter

object TodayCourseDisplayFormatter {
    private const val EMPTY_TEACHER_TEXT = "暂无教师信息"
    private const val EMPTY_LOCATION_TEXT = "暂无地点信息"
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatTeacherLine(teacher: String): String = teacher.trim().ifEmpty { EMPTY_TEACHER_TEXT }

    fun formatTimeLine(course: CourseData): String {
        return "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
    }

    fun formatLocationLine(location: String): String {
        return location.trim().ifEmpty { EMPTY_LOCATION_TEXT }
    }

    fun formatWidgetTimeAndLocationLine(
        timeText: String,
        location: String
    ): String {
        return "$timeText | ${formatLocationLine(location)}"
    }
}
