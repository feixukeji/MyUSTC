package com.feixu.myustc

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ExamCountdown(
    val value: String,
    val unit: String,
    val text: String
)

data class ExamDisplayItem(
    val exam: Exam,
    val countdown: ExamCountdown,
    val startTime: LocalDateTime?,
    val classroom: String
) {
    val name: String = exam.name
    val time: String = exam.time
}

object ExamHelper {
    private val examTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun readExams(context: Context): List<Exam> {
        return try {
            FileHelper.read(context, "Exam.fx")
                .split(';')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { item ->
                    val examData = item.split(',', limit = 3)
                    if (examData.size < 3) {
                        null
                    } else {
                        Exam(
                            examData[0].trim(),
                            examData[1].trim(),
                            examData[2].trim()
                        )
                    }
                }
                .sortedWith(compareBy({ parseStartTime(it) ?: LocalDateTime.MAX }, { it.name }))
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getUpcomingExams(context: Context, limit: Int = Int.MAX_VALUE): List<Exam> {
        val current = LocalDateTime.now()
        return readExams(context)
            .filter { exam ->
                val startTime = parseStartTime(exam)
                startTime != null && current < startTime
            }
            .take(limit)
    }

    fun getUpcomingExamDisplayItems(context: Context, limit: Int = Int.MAX_VALUE): List<ExamDisplayItem> {
        return getUpcomingExams(context, limit).map { toDisplayItem(it) }
    }

    fun toDisplayItem(exam: Exam, current: LocalDateTime = LocalDateTime.now()): ExamDisplayItem {
        return ExamDisplayItem(
            exam = exam,
            countdown = formatCountdown(exam, current),
            startTime = parseStartTime(exam),
            classroom = formatClassroom(exam)
        )
    }

    fun parseStartTime(exam: Exam): LocalDateTime? {
        return try {
            val startTimeText = exam.time.substringBefore('~').trim()
            LocalDateTime.parse(startTimeText, examTimeFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun parseEndTime(exam: Exam): LocalDateTime? {
        return try {
            val timeRange = exam.time.split("~")
            val startTimeText = timeRange[0].trim()
            val endTimeText = if (timeRange.size > 1) {
                val date = startTimeText.substringBefore(' ')
                "$date ${timeRange[1].trim()}"
            } else {
                startTimeText
            }
            LocalDateTime.parse(endTimeText, examTimeFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun formatClassroom(exam: Exam): String {
        return exam.classroom.ifBlank { "暂无地点信息" }
    }

    fun formatCountdown(exam: Exam, current: LocalDateTime = LocalDateTime.now()): ExamCountdown {
        val examTime = parseStartTime(exam)
            ?: return ExamCountdown("--", "", "时间解析错误")

        val countdownDays = ChronoUnit.DAYS.between(current, examTime)
        return when {
            countdownDays > 0 -> {
                val currentDay = LocalDate.now()
                val examDay = examTime.toLocalDate()
                val days = ChronoUnit.DAYS.between(currentDay, examDay).coerceAtLeast(0)
                ExamCountdown(days.toString(), "天", "${days}天")
            }

            else -> {
                val countdownHours = ChronoUnit.HOURS.between(current, examTime)
                if (countdownHours > 0) {
                    ExamCountdown(countdownHours.toString(), "小时", "${countdownHours}小时")
                } else {
                    val countdownMinutes = ChronoUnit.MINUTES.between(current, examTime)
                    if (countdownMinutes > 0) {
                        ExamCountdown(countdownMinutes.toString(), "分钟", "${countdownMinutes}分钟")
                    } else {
                        ExamCountdown("0", "分钟", "考试已开始")
                    }
                }
            }
        }
    }

    fun scheduleReminders(context: Context, exams: List<Exam> = getUpcomingExams(context)) {
        val settingPersonalize = context.getSharedPreferences("Personalize", Context.MODE_PRIVATE)
        val remindBefore = settingPersonalize.getInt("RemindBefore", 0)
        if (remindBefore == 0) return

        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        exams.forEach { exam ->
            val examTime = parseStartTime(exam) ?: return@forEach
            val startTime = examTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - remindBefore * 60000L
            if (now < startTime) {
                val intent = Intent(context, RemindReceiver::class.java)
                    .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setPackage(context.packageName)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    exam.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pendingIntent)
            }
        }
    }
}
