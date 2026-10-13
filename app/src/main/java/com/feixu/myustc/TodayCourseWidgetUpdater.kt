package com.feixu.myustc

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * 用于处理桌面小组件定时更新的类
 */
class TodayCourseWidgetUpdater : BroadcastReceiver() {

    companion object {
        const val ACTION_AUTO_UPDATE_WIDGET = "com.feixu.myustc.ACTION_AUTO_UPDATE_WIDGET"
        const val ACTION_MANUAL_REFRESH = "com.feixu.myustc.ACTION_MANUAL_REFRESH"

        private const val UPDATE_INTERVAL = 60 * 1000L // 下一分钟边界；系统可能延迟非精确闹钟
        private const val WAKELOCK_TIMEOUT = 3 * 1000L // 3秒超时

        fun startPeriodicUpdate(context: Context) {
            scheduleNextUpdate(context)
        }

        fun ensurePeriodicUpdate(context: Context) {
            if (hasManagedWidgets(context)) {
                startPeriodicUpdate(context)
            }
        }

        fun stopPeriodicUpdateIfNoManagedWidgets(context: Context) {
            if (!hasManagedWidgets(context)) {
                stopPeriodicUpdate(context)
            }
        }

        private fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val now = System.currentTimeMillis()
            val nextUpdateTime = (now / UPDATE_INTERVAL + 1) * UPDATE_INTERVAL
            alarmManager.set(
                AlarmManager.RTC,
                nextUpdateTime,
                createUpdatePendingIntent(context)
            )
        }

        fun stopPeriodicUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createUpdatePendingIntent(context)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        private fun createUpdatePendingIntent(context: Context): PendingIntent {
            val intent = Intent(ACTION_AUTO_UPDATE_WIDGET).apply {
                setClass(context, TodayCourseWidgetUpdater::class.java)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            return PendingIntent.getBroadcast(
                context.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun hasManagedWidgets(context: Context): Boolean {
            return try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TodayCourseWidget::class.java)
                ).isNotEmpty() || appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ExamInfoWidget::class.java)
                ).isNotEmpty() || appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TaskListWidget::class.java)
                ).isNotEmpty()
            } catch (_: Exception) {
                false
            }
        }

        fun updateManagedWidgets(context: Context): Boolean {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val courseWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TodayCourseWidget::class.java)
                )
                val examWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ExamInfoWidget::class.java)
                )
                val taskWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TaskListWidget::class.java)
                )

                if (courseWidgetIds.isNotEmpty()) {
                    for (appWidgetId in courseWidgetIds) {
                        runCatching {
                            TodayCourseWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                }

                if (examWidgetIds.isNotEmpty()) {
                    for (appWidgetId in examWidgetIds) {
                        runCatching {
                            ExamInfoWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                }

                if (taskWidgetIds.isNotEmpty()) {
                    for (appWidgetId in taskWidgetIds) {
                        runCatching {
                            TaskListWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                }

                return courseWidgetIds.isNotEmpty() || examWidgetIds.isNotEmpty() ||
                    taskWidgetIds.isNotEmpty()
            } catch (_: Exception) {
                // Keep the one-shot chain alive and retry after transient framework failures.
                return true
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyUSTC:WidgetUpdateWakeLock"
        )

        try {
            wakeLock.acquire(WAKELOCK_TIMEOUT)

            when (intent.action) {
                ACTION_AUTO_UPDATE_WIDGET -> {
                    if (updateManagedWidgets(context)) {
                        scheduleNextUpdate(context)
                    } else {
                        stopPeriodicUpdate(context)
                    }
                }

                ACTION_MANUAL_REFRESH -> {
                    updateManagedWidgets(context)
                }

                Intent.ACTION_BOOT_COMPLETED -> {
                    if (updateManagedWidgets(context)) {
                        startPeriodicUpdate(context)
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
