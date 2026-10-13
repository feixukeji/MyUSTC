package com.feixu.myustc

import androidx.core.graphics.toColorInt
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 课程信息数据类
 * 支持教务系统课程和自定义课程的统一展示
 */
data class CourseData(
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int, // 1-7 (周一到周日)
    val startTime: LocalTime, // 开始时间
    val endTime: LocalTime, // 结束时间
    val startUnit: Int, // 开始节数 (1-13)
    val endUnit: Int, // 结束节数 (1-13)
    val weeks: List<Int> = emptyList(), // 上课周次
    val color: Int = COURSE_COLORS.first() // 课程颜色
) {
    /**
     * 检查课程是否在指定周显示
     */
    fun isInWeek(week: Int): Boolean {
        if (weeks.isEmpty()) return true
        return weeks.contains(week)
    }

    fun unitRangeText(): String {
        return "$startUnit-$endUnit"
    }

    fun simplifiedWeeksText(): String {
        return simplifyWeeks(weeks)
    }

    fun simplifiedWeeksWithSuffixText(): String {
        val text = simplifiedWeeksText()
        return if (text == "全周" || text.endsWith("周")) text else "${text}周"
    }

    companion object {
        // 课程时间表（科大标准作息时间）
        val TIME_TABLE = mapOf(
            1 to (LocalTime.of(7, 50) to LocalTime.of(8, 35)),
            2 to (LocalTime.of(8, 40) to LocalTime.of(9, 25)),
            3 to (LocalTime.of(9, 45) to LocalTime.of(10, 30)),
            4 to (LocalTime.of(10, 35) to LocalTime.of(11, 20)),
            5 to (LocalTime.of(11, 25) to LocalTime.of(12, 10)),
            6 to (LocalTime.of(14, 0) to LocalTime.of(14, 45)),
            7 to (LocalTime.of(14, 50) to LocalTime.of(15, 35)),
            8 to (LocalTime.of(15, 55) to LocalTime.of(16, 40)),
            9 to (LocalTime.of(16, 45) to LocalTime.of(17, 30)),
            10 to (LocalTime.of(17, 35) to LocalTime.of(18, 20)),
            11 to (LocalTime.of(19, 30) to LocalTime.of(20, 15)),
            12 to (LocalTime.of(20, 20) to LocalTime.of(21, 5)),
            13 to (LocalTime.of(21, 10) to LocalTime.of(21, 55))
        )

        private val COURSE_COLORS = listOf(
            "#2196F3".toColorInt(), // 天蓝
            "#FF4D4F".toColorInt(), // 鲜红
            "#00B875".toColorInt(), // 翠绿
            "#FA8714".toColorInt(), // 鲜橙
            "#9654E8".toColorInt(), // 明紫

            "#00D5C0".toColorInt(), // 海沫青
            "#F72585".toColorInt(), // 覆盆子粉
            "#F6C900".toColorInt(), // 金黄
            "#00BCD4".toColorInt(), // 青蓝
            "#FF8A65".toColorInt(), // 鲑红

            "#4768EB".toColorInt(), // 亮靛蓝
            "#005BFF".toColorInt(), // 电光蓝
            "#FF80AB".toColorInt(), // 亮玫粉
            "#59E90C".toColorInt(), // 青柠绿
            "#EBB147".toColorInt(), // 蜂蜜金

            "#F53D00".toColorInt(), // 朱砂橙
            "#F50025".toColorInt(), // 猩红
            "#0CE959".toColorInt(), // 春绿
            "#EA80FC".toColorInt(), // 兰花紫
            "#B8E000".toColorInt(), // 酸橙绿
        )

        fun assignColors(courses: List<CourseData>): List<CourseData> {
            val colorsByName = mutableMapOf<String, Int>()
            return courses.map { course ->
                val color = colorsByName.getOrPut(course.name.trim()) {
                    COURSE_COLORS[colorsByName.size % COURSE_COLORS.size]
                }
                course.copy(color = color)
            }
        }

        // 从节次获取时间
        fun getTimeFromUnit(unit: Int): Pair<LocalTime, LocalTime>? {
            return TIME_TABLE[unit]
        }

        fun getUnitRangeCoveredAtLeastHalf(startTime: LocalTime, endTime: LocalTime): IntRange? {
            if (!endTime.isAfter(startTime)) return null

            val coveredUnits = TIME_TABLE.entries
                .filter { (_, times) ->
                    val unitDurationSeconds = ChronoUnit.SECONDS.between(times.first, times.second)
                    val coveredSeconds = overlapSeconds(startTime, endTime, times.first, times.second)
                    coveredSeconds * 2 >= unitDurationSeconds
                }
                .map { it.key }

            if (coveredUnits.isEmpty()) return null
            return coveredUnits.minOrNull()!!..coveredUnits.maxOrNull()!!
        }

        fun simplifyWeeks(weeks: List<Int>): String {
            if (weeks.isEmpty()) return "全周"

            val sorted = weeks.distinct().sorted()
            val ranges = mutableListOf<String>()
            var start = sorted.first()
            var prev = sorted.first()

            for (i in 1 until sorted.size) {
                val current = sorted[i]
                if (current == prev + 1) {
                    prev = current
                    continue
                }

                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = current
                prev = current
            }

            ranges.add(if (start == prev) "$start" else "$start-$prev")
            return ranges.joinToString(",")
        }

        // 将Course转换为CourseData
        fun fromCourse(course: Course): CourseData {
            val times = getTimeFromUnit(course.startUnit) ?: (LocalTime.of(8, 0) to LocalTime.of(9, 0))
            val endTimes = getTimeFromUnit(course.endUnit) ?: (LocalTime.of(9, 0) to LocalTime.of(10, 0))
            val weeks = CourseWeekParser.parseWeeks(course.week)

            return CourseData(
                name = course.name,
                teacher = course.teacher,
                location = course.classroom,
                dayOfWeek = course.day,
                startTime = times.first,
                endTime = endTimes.second,
                startUnit = course.startUnit,
                endUnit = course.endUnit,
                weeks = weeks
            )
        }

        fun fromSchoolCourse(course: SchoolCourse): CourseData? {
            val hasExplicitTimeRange = course.startUnit == 0 && course.endUnit == 0
            val startTime: LocalTime
            val endTime: LocalTime
            val displayStartUnit: Int
            val displayEndUnit: Int

            if (hasExplicitTimeRange) {
                startTime = parseTimeOfDay(course.startDate) ?: return null
                endTime = parseTimeOfDay(course.endDate) ?: return null
                val coveredUnitRange = getUnitRangeCoveredAtLeastHalf(startTime, endTime) ?: return null
                displayStartUnit = coveredUnitRange.first
                displayEndUnit = coveredUnitRange.last
            } else {
                startTime = getTimeFromUnit(course.startUnit)?.first ?: return null
                endTime = getTimeFromUnit(course.endUnit)?.second ?: return null
                displayStartUnit = course.startUnit
                displayEndUnit = course.endUnit
            }

            val weeks = CourseWeekParser.parseWeeks(course.week)

            return CourseData(
                name = course.name,
                teacher = course.teacher,
                location = course.classroom,
                dayOfWeek = course.day,
                startTime = startTime,
                endTime = endTime,
                startUnit = displayStartUnit,
                endUnit = displayEndUnit,
                weeks = weeks
            )
        }

        private fun overlapSeconds(
            firstStart: LocalTime,
            firstEnd: LocalTime,
            secondStart: LocalTime,
            secondEnd: LocalTime
        ): Long {
            val overlapStart = if (firstStart.isAfter(secondStart)) firstStart else secondStart
            val overlapEnd = if (firstEnd.isBefore(secondEnd)) firstEnd else secondEnd
            return if (overlapEnd.isAfter(overlapStart)) {
                ChronoUnit.SECONDS.between(overlapStart, overlapEnd)
            } else {
                0L
            }
        }

        private val timeOfDayRegex = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")
        private val timeFormatters = listOf(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_TIME
        )

        private fun parseTimeOfDay(raw: String): LocalTime? {
            val cleaned = raw.trim()
            if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) return null

            val candidate = timeOfDayRegex.find(cleaned)?.value ?: cleaned
            for (formatter in timeFormatters) {
                try {
                    return LocalTime.parse(candidate, formatter)
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}
