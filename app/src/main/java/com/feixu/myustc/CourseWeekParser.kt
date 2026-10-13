package com.feixu.myustc

object CourseWeekParser {
    private val numberRegex = Regex("\\d+")

    fun parseWeeks(weekText: String): List<Int> {
        val normalized = weekText
            .replace('，', ',')
            .replace('、', ',')
            .replace('；', ',')
            .replace(';', ',')
            .replace("（", "(")
            .replace("）", ")")
            .replace("第", "")
            .trim()

        if (normalized.isEmpty()) return emptyList()

        val weeks = mutableListOf<Int>()

        normalized.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { rawPart ->
                val parity = when {
                    rawPart.contains("单") -> WeekParity.ODD
                    rawPart.contains("双") -> WeekParity.EVEN
                    else -> null
                }
                val numbers = numberRegex.findAll(rawPart).mapNotNull { it.value.toIntOrNull() }.toList()
                val parsedWeeks = when (numbers.size) {
                    0 -> emptyList()
                    1 -> listOf(numbers[0])
                    else -> {
                        val start = numbers[0]
                        val end = numbers[1]
                        if (start <= end) (start..end).toList() else (end..start).toList()
                    }
                }.filter { week ->
                    parity == null || parity.matches(week)
                }

                if (parsedWeeks.isNotEmpty()) {
                    weeks.addAll(parsedWeeks)
                }
            }

        return weeks.distinct().sorted()
    }

    private enum class WeekParity {
        ODD,
        EVEN;

        fun matches(week: Int): Boolean {
            return when (this) {
                ODD -> week % 2 == 1
                EVEN -> week % 2 == 0
            }
        }
    }
}
