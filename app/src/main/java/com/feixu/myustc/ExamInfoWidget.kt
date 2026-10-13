package com.feixu.myustc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 考试信息桌面小组件
 */
class ExamInfoWidget : AppWidgetProvider() {

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

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } else {
                refreshAllWidgets(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
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
        TodayCourseWidgetUpdater.stopPeriodicUpdateIfNoManagedWidgets(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.feixu.myustc.ACTION_REFRESH_EXAM_WIDGET"
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ExamInfoWidget::class.java)
            )
            if (appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_exam_info)
            val exams = ExamHelper.getUpcomingExamDisplayItems(context)
            WidgetCollectionRemoteViews.setExamAdapter(
                context,
                views,
                appWidgetId,
                exams
            )

            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            views.setTextViewText(R.id.widget_exam_last_update, "上次刷新: ${dateFormat.format(Date())}")
            views.setTextViewText(
                R.id.widget_exam_subtitle,
                if (exams.isEmpty()) "" else "${exams.size} 场考试"
            )

            val refreshIntent = Intent(ACTION_REFRESH_WIDGET).apply {
                component = ComponentName(context, ExamInfoWidget::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_exam_refresh_button, refreshPendingIntent)

            val openExamPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 200000,
                Intent(context, ExamArrangeActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_exam_title, openExamPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_exam_subtitle, openExamPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_exam_last_update, openExamPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_exam_blank_area, openExamPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_exam_empty_view, openExamPendingIntent)
            views.setPendingIntentTemplate(R.id.widget_exam_list, openExamPendingIntent)

            if (exams.isEmpty()) {
                views.setViewVisibility(R.id.widget_exam_list, View.GONE)
                views.setViewVisibility(R.id.widget_exam_empty_view, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_exam_list, View.VISIBLE)
                views.setViewVisibility(R.id.widget_exam_empty_view, View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
