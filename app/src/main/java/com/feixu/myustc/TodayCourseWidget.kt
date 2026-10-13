package com.feixu.myustc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/**
 * 今日课程桌面小组件
 */
class TodayCourseWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        TodayCourseWidgetUpdater.startPeriodicUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // 获取触发事件的小组件ID
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            when (intent.action) {
                ACTION_REFRESH_WIDGET -> {
                    // 刷新按钮 - 回到当前日期，只更新指定的小组件
                    saveCurrentDisplayDate(context, appWidgetId, LocalDate.now())
                    updateLastOperationTime(context, appWidgetId) // 更新操作时间
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }

                ACTION_PREV_DAY -> {
                    // 前一天按钮 - 只更新指定的小组件
                    val currentDate = getCurrentDisplayDate(context, appWidgetId)
                    val newDate = currentDate.minusDays(1)
                    saveCurrentDisplayDate(context, appWidgetId, newDate)
                    updateLastOperationTime(context, appWidgetId) // 更新操作时间
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }

                ACTION_NEXT_DAY -> {
                    // 后一天按钮 - 只更新指定的小组件
                    val currentDate = getCurrentDisplayDate(context, appWidgetId)
                    val newDate = currentDate.plusDays(1)
                    saveCurrentDisplayDate(context, appWidgetId, newDate)
                    updateLastOperationTime(context, appWidgetId) // 更新操作时间
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // 当小组件被删除时，清理该小组件的存储数据
        for (appWidgetId in appWidgetIds) {
            clearWidgetPreferences(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 第一个小组件被创建时，启动定时更新
        TodayCourseWidgetUpdater.startPeriodicUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDisabled(context: Context) {
        // 最后一个小组件被移除时，停止定时更新
        TodayCourseWidgetUpdater.stopPeriodicUpdateIfNoManagedWidgets(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.feixu.myustc.ACTION_REFRESH_WIDGET"
        const val ACTION_PREV_DAY = "com.feixu.myustc.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.feixu.myustc.ACTION_NEXT_DAY"

        // 存储Widget设置的SharedPreferences名称
        private const val WIDGET_PREFS = "widget_preferences"

        // 当前显示日期的偏好设置键前缀
        private const val PREF_DISPLAY_DATE = "display_date"

        // 最后操作时间的偏好设置键前缀
        private const val PREF_LAST_OPERATION_TIME = "last_operation_time"

        // 自动刷新延迟（毫秒）- 1分钟
        private const val AUTO_REFRESH_DELAY = 60 * 1000L
        private fun getWidgetPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        }

        private fun getDisplayDateKey(appWidgetId: Int): String {
            return "${PREF_DISPLAY_DATE}_$appWidgetId"
        }

        private fun getLastOperationTimeKey(appWidgetId: Int): String {
            return "${PREF_LAST_OPERATION_TIME}_$appWidgetId"
        }

        private fun getCurrentDisplayDate(context: Context, appWidgetId: Int): LocalDate {
            val date = getWidgetPrefs(context).getString(getDisplayDateKey(appWidgetId), "")
            return try {
                date?.takeIf(String::isNotEmpty)?.let(LocalDate::parse) ?: LocalDate.now()
            } catch (_: Exception) {
                LocalDate.now()
            }
        }

        private fun saveCurrentDisplayDate(
            context: Context,
            appWidgetId: Int,
            date: LocalDate
        ) {
            getWidgetPrefs(context).edit {
                putString(getDisplayDateKey(appWidgetId), date.toString())
            }
        }

        private fun updateLastOperationTime(context: Context, appWidgetId: Int) {
            getWidgetPrefs(context).edit {
                putLong(getLastOperationTimeKey(appWidgetId), System.currentTimeMillis())
            }
        }

        private fun shouldResetToToday(context: Context, appWidgetId: Int): Boolean {
            val lastOperationTime = getWidgetPrefs(context)
                .getLong(getLastOperationTimeKey(appWidgetId), 0)
            return System.currentTimeMillis() - lastOperationTime > AUTO_REFRESH_DELAY
        }

        private fun clearWidgetPreferences(context: Context, appWidgetId: Int) {
            getWidgetPrefs(context).edit {
                remove(getDisplayDateKey(appWidgetId))
                    .remove(getLastOperationTimeKey(appWidgetId))
            }
        }

        /**
         * 计算当前周数
         */
        fun calculateWeekNumber(context: Context, date: LocalDate): Long {
            return TodayCourseProvider.calculateWeekNumber(context, date)?.toLong() ?: -1
        }

        /**
         * 立即刷新所有已添加的小组件实例
         */
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TodayCourseWidget::class.java)
            )
            if (appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        // 更新小组件
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_course)

            if (shouldResetToToday(context, appWidgetId)) {
                saveCurrentDisplayDate(context, appWidgetId, LocalDate.now())
            }

            // 获取当前显示日期 - 使用小组件ID获取独立状态
            val displayDate = getCurrentDisplayDate(context, appWidgetId)
            val isToday = displayDate == LocalDate.now()

            // 设置日期标题
            val dateText = if (isToday) {
                "今日 - ${displayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)}"
            } else {
                "${displayDate.format(DateTimeFormatter.ofPattern("MM/dd"))} ${displayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)}"
            }
            views.setTextViewText(R.id.widget_date, dateText)

            // 设置周数显示
            val currentWeek = calculateWeekNumber(context, displayDate)
            val weekText = if (currentWeek > 0) "第${currentWeek}周" else "假期中"
            views.setTextViewText(R.id.widget_current_week, weekText)

            // 更新最后刷新时间
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lastUpdateText = "上次刷新: ${dateFormat.format(Date())}"
            views.setTextViewText(R.id.widget_last_update, lastUpdateText)

            // 设置刷新按钮点击事件（回到今天）- 使用小组件ID
            val refreshIntent = Intent(ACTION_REFRESH_WIDGET).apply {
                component = ComponentName(context, TodayCourseWidget::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId, // 使用appWidgetId作为请求码，确保每个小组件有独立的PendingIntent
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // 设置前一天按钮点击事件 - 使用小组件ID
            val prevDayIntent = Intent(ACTION_PREV_DAY).apply {
                component = ComponentName(context, TodayCourseWidget::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevDayPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 100000, // 使用appWidgetId+偏移量作为请求码，避免与其他按钮冲突
                prevDayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev_day, prevDayPendingIntent)

            // 设置后一天按钮点击事件 - 使用小组件ID
            val nextDayIntent = Intent(ACTION_NEXT_DAY).apply {
                component = ComponentName(context, TodayCourseWidget::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextDayPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 200000, // 使用appWidgetId+偏移量作为请求码，避免与其他按钮冲突
                nextDayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_day, nextDayPendingIntent)

            // 获取所选日期的课程
            val courses = if (currentWeek > 0) {
                getCoursesForDate(context, displayDate)
            } else {
                emptyList() // 假期中不显示任何课程
            }
            WidgetCollectionRemoteViews.setTodayCourseAdapter(
                context,
                views,
                appWidgetId,
                displayDate,
                courses
            )

            val openCoursePendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 300000,
                Intent(context, CourseTableActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_date, openCoursePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_current_week, openCoursePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_last_update, openCoursePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_course_blank_area, openCoursePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, openCoursePendingIntent)
            views.setPendingIntentTemplate(R.id.widget_course_list, openCoursePendingIntent)

            // 如果没有课程，显示空视图
            if (courses.isEmpty()) {
                views.setViewVisibility(R.id.widget_course_list, View.GONE)
                views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_empty_view, if (currentWeek > 0) {
                        if (isToday) "今天没有课程" else "这一天没有课程"
                    } else {
                        "假期愉快"
                    }
                )
            } else {
                views.setViewVisibility(R.id.widget_course_list, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty_view, View.GONE)
            }

            // 更新小组件
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // 获取特定日期的课程列表
        fun getCoursesForDate(context: Context, date: LocalDate): List<TodayCourseItem> {
            return TodayCourseProvider.getCoursesForDate(context, date)
                .map { course -> TodayCourseItem.fromCourseData(course) }
        }
    }
}

// 小组件显示的课程项数据类
data class TodayCourseItem(
    val name: String,
    val time: String,
    val location: String,
    val teacher: String
) {
    companion object {
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun fromCourseData(course: CourseData): TodayCourseItem {
            return TodayCourseItem(
                name = course.name,
                time = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}",
                location = course.location,
                teacher = course.teacher
            )
        }
    }
}
