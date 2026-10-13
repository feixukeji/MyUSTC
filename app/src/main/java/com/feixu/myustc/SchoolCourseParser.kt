package com.feixu.myustc

import android.content.Context
import org.json.JSONObject

/**
 * 学校课程解析器
 * 用于解析从教务系统获取的 JSON 课程数据
 */
object SchoolCourseParser {

    private const val SCHOOL_COURSE_JSON_FILE = "courses.json"

    /**
     * 从本地 JSON 课程表文件中解析课程数据。
     */
    fun parseSchoolCourses(context: Context): List<SchoolCourse> {
        return parseSchoolCoursesFromJson(FileHelper.read(context, SCHOOL_COURSE_JSON_FILE))
    }

    /**
     * 从教务系统返回的 JSON 课程表数据中解析课程。
     */
    fun parseSchoolCoursesFromJson(json: String): List<SchoolCourse> {
        return try {
            val trimmed = json.trim()
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) return emptyList()
            extractCoursesFromJson(JSONObject(trimmed))
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 从教务 JSON 文档中提取课程信息。
     */
    private fun extractCoursesFromJson(root: JSONObject): List<SchoolCourse> {
        val courses = mutableListOf<SchoolCourse>()

        try {
            val studentTableVm = root.optJSONObject("studentTableVm") ?: return emptyList()
            val activities = studentTableVm.optJSONArray("activities") ?: return emptyList()

            for (i in 0 until activities.length()) {
                val activity = activities.optJSONObject(i) ?: continue
                val dayOfWeek = activity.optInt("weekday", -1)
                val startUnit = activity.optInt("startUnit", -1)
                val endUnit = activity.optInt("endUnit", -1)
                val startDate = sanitizeNullableText(activity.optString("startDate", ""))
                val endDate = sanitizeNullableText(activity.optString("endDate", ""))
                val hasUnitRange = startUnit > 0 && endUnit > 0
                val hasExplicitTimeRange = startUnit == 0 && endUnit == 0 && startDate.isNotEmpty() && endDate.isNotEmpty()
                if (dayOfWeek !in 1..7 || (!hasUnitRange && !hasExplicitTimeRange)) continue

                val courseName = activity.optString("courseName", "").trim().ifEmpty {
                    activity.optString("lessonName", "").trim().ifEmpty {
                        activity.optString("lessonCode", "").trim()
                    }
                }
                if (courseName.isEmpty()) continue

                courses.add(
                    SchoolCourse(
                        name = courseName,
                        teacher = TeacherFormatter.format(extractTeachersFromJson(activity)),
                        classroom = buildLocationFromJson(activity),
                        week = normalizeWeekText(activity),
                        day = dayOfWeek,
                        startUnit = startUnit,
                        endUnit = endUnit,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
            }
        } catch (_: Exception) {
        }

        return courses
    }

    private fun extractTeachersFromJson(activity: JSONObject): List<String> {
        val teachers = mutableListOf<String>()

        val teacherArray = activity.optJSONArray("teachers")
        if (teacherArray != null) {
            for (i in 0 until teacherArray.length()) {
                val teacher = sanitizeNullableText(teacherArray.optString(i, ""))
                if (teacher.isNotEmpty()) {
                    teachers.add(teacher)
                }
            }
        }

        if (teachers.isNotEmpty()) {
            return teachers
        }

        val teacherDeepVms = activity.optJSONArray("teacherDeepVms")
        if (teacherDeepVms != null) {
            for (i in 0 until teacherDeepVms.length()) {
                val teacherVm = teacherDeepVms.optJSONObject(i) ?: continue
                val person = teacherVm.optJSONObject("person") ?: continue
                val name = sanitizeNullableText(person.optString("nameZh", "")).ifEmpty {
                    sanitizeNullableText(person.optString("nameEn", ""))
                }
                if (name.isNotEmpty()) {
                    teachers.add(name)
                }
            }
        }

        return teachers
    }

    private fun buildLocationFromJson(activity: JSONObject): String {
        val room = sanitizeNullableText(activity.optString("room", ""))
        if (room.isNotEmpty()) return room

        val roomCode = sanitizeNullableText(activity.optString("roomCode", ""))
        if (roomCode.isNotEmpty()) return roomCode

        return sanitizeNullableText(activity.optString("customPlace", ""))
    }

    private fun sanitizeNullableText(value: String?): String {
        if (value == null) return ""
        val cleaned = value.trim()
        return if (cleaned.equals("null", ignoreCase = true)) "" else cleaned
    }

    private fun normalizeWeekText(activity: JSONObject): String {
        val weeksStr = activity.optString("weeksStr", "").trim()
        if (weeksStr.isNotEmpty()) return weeksStr

        val weeksArray = activity.optJSONArray("weeksArray") ?: return ""
        val weeks = mutableListOf<Int>()
        for (i in 0 until weeksArray.length()) {
            val week = weeksArray.optInt(i, -1)
            if (week > 0) {
                weeks.add(week)
            }
        }

        if (weeks.isEmpty()) return ""
        return CourseData.simplifyWeeks(weeks)
    }
}

/**
 * 学校课程数据类
 */
data class SchoolCourse(
    val name: String,
    val teacher: String,
    val classroom: String,
    val week: String,
    val day: Int,        // 1-7 对应周一至周日
    val startUnit: Int,  // 开始节数
    val endUnit: Int,    // 结束节数
    val startDate: String = "", // 教务系统自定义开始时间，例如 19:00
    val endDate: String = ""    // 教务系统自定义结束时间，例如 21:55
)
