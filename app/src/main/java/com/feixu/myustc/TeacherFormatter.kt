package com.feixu.myustc

object TeacherFormatter {
    private const val MAX_TEACHER_DISPLAY_COUNT = 3
    private const val TEACHER_SEPARATOR = "、"

    fun format(teachers: Iterable<String>): String {
        val normalizedTeachers = teachers
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            .distinct()

        if (normalizedTeachers.isEmpty()) return ""

        val suffix = if (normalizedTeachers.size > MAX_TEACHER_DISPLAY_COUNT) "等" else ""
        return normalizedTeachers
            .take(MAX_TEACHER_DISPLAY_COUNT)
            .joinToString(TEACHER_SEPARATOR) + suffix
    }

    /**
     * 自定义课程使用顿号分隔多位教师；姓名内部的空格或逗号不会被拆分。
     */
    fun format(rawTeacher: String): String {
        val normalized = rawTeacher.trim()
        if (normalized.isEmpty()) return ""
        return format(normalized.split(TEACHER_SEPARATOR))
    }
}
