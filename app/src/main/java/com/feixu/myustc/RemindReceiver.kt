package com.feixu.myustc

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.Duration
import java.time.LocalDateTime

class RemindReceiver : BroadcastReceiver() {
    //考试提醒接收器
    override fun onReceive(context: Context, intent: Intent) {
        val settingPersonalize = context.getSharedPreferences("Personalize", Context.MODE_PRIVATE)
        val remindBefore = settingPersonalize.getInt("RemindBefore", 0)
        if (remindBefore == 0) return

        val current = LocalDateTime.now()
        ExamHelper.getUpcomingExams(context).forEach { exam ->
            val examTime = ExamHelper.parseStartTime(exam) ?: return@forEach
            if (current < examTime) {
                val duration = Duration.between(current, examTime).toMinutes()
                if (duration <= remindBefore) {
                    val it = Intent(context, MainActivity::class.java)
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val builder = NotificationCompat.Builder(context, "1")
                        .setSmallIcon(R.drawable.ic_notification_exam_reminder)
                        .setContentTitle("考试提醒")
                        .setContentText(exam.name + "考试将在" + duration.toString() + "分钟后开始！")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                    (context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).notify(1, builder.build())
                }
                return
            }
        }
    }
}
