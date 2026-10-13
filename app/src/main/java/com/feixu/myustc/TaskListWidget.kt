package com.feixu.myustc

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TaskListWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
        TodayCourseWidgetUpdater.startPeriodicUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> updateWidget(context, intent)
            ACTION_TASK_ITEM -> handleTaskAction(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
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
    }

    private fun updateWidget(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            refreshAllWidgets(context)
        } else {
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    private fun handleTaskAction(context: Context, intent: Intent) {
        when {
            intent.hasExtra(EXTRA_COMPLETE_TASK_POSITION) -> TasksHelper.completeTask(
                context,
                intent.getIntExtra(EXTRA_COMPLETE_TASK_POSITION, -1)
            )

            intent.hasExtra(TaskListActivity.EXTRA_EDIT_TASK_INDEX) -> openTaskList(
                context,
                intent.getIntExtra(TaskListActivity.EXTRA_EDIT_TASK_INDEX, -1)
            )

            else -> openTaskList(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.feixu.myustc.ACTION_REFRESH_TASK_WIDGET"
        private const val ACTION_TASK_ITEM = "com.feixu.myustc.ACTION_TASK_WIDGET_ITEM"
        internal const val EXTRA_COMPLETE_TASK_POSITION = "complete_task_position"

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.getAppWidgetIds(
                ComponentName(context, TaskListWidget::class.java)
            ).forEach { updateAppWidget(context, appWidgetManager, it) }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val tasks = TasksHelper.readTasks(context)
                .withIndex()
                .filterNot { it.value.ok }
            val views = RemoteViews(context.packageName, R.layout.widget_task_list)

            WidgetCollectionRemoteViews.setTaskAdapter(
                context,
                views,
                appWidgetId,
                tasks
            )

            views.setTextViewText(R.id.widget_task_subtitle, "${tasks.size} 项待完成")
            views.setTextViewText(
                R.id.widget_task_last_update,
                "上次刷新: ${LocalTime.now().format(timeFormatter)}"
            )
            views.setViewVisibility(
                R.id.widget_task_list,
                if (tasks.isEmpty()) View.GONE else View.VISIBLE
            )
            views.setViewVisibility(
                R.id.widget_task_empty_view,
                if (tasks.isEmpty()) View.VISIBLE else View.GONE
            )

            val openTasks = activityPendingIntent(
                context,
                appWidgetId * 100,
                taskListIntent(context)
            )
            views.setOnClickPendingIntent(R.id.widget_task_title, openTasks)
            views.setOnClickPendingIntent(R.id.widget_task_subtitle, openTasks)
            views.setOnClickPendingIntent(R.id.widget_task_last_update, openTasks)
            views.setOnClickPendingIntent(R.id.widget_task_blank_area, openTasks)
            views.setOnClickPendingIntent(R.id.widget_task_empty_view, openTasks)

            views.setOnClickPendingIntent(
                R.id.widget_task_add_button,
                activityPendingIntent(
                    context,
                    appWidgetId * 100 + 1,
                    taskListIntent(context)
                        .putExtra(TaskListActivity.EXTRA_ADD_TASK, true)
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_task_refresh_button,
                broadcastPendingIntent(
                    context,
                    appWidgetId * 100 + 2,
                    Intent(ACTION_REFRESH).apply {
                        component = ComponentName(context, TaskListWidget::class.java)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                )
            )
            views.setPendingIntentTemplate(
                R.id.widget_task_list,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 3,
                    Intent(ACTION_TASK_ITEM).apply {
                        component = ComponentName(context, TaskListWidget::class.java)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun activityPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent
        ) = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            backgroundActivityOptions(forCreator = true)
        )

        private fun taskListIntent(context: Context) =
            Intent(context, TaskListActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

        private fun openTaskList(context: Context, position: Int? = null) {
            val intent = taskListIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (position != null && position >= 0) {
                intent.putExtra(TaskListActivity.EXTRA_EDIT_TASK_INDEX, position)
            }
            val pendingIntent = activityPendingIntent(
                context,
                1_000_000 + (position ?: -1),
                intent
            )
            val sendOptions = backgroundActivityOptions(forCreator = false)
            if (sendOptions == null) {
                pendingIntent.send()
            } else {
                pendingIntent.send(context, 0, null, null, null, null, sendOptions)
            }
        }

        @Suppress("DEPRECATION")
        private fun backgroundActivityOptions(forCreator: Boolean): Bundle? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
            val mode = if (Build.VERSION.SDK_INT >= 36) {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            } else {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            return ActivityOptions.makeBasic().apply {
                if (forCreator) {
                    pendingIntentCreatorBackgroundActivityStartMode = mode
                } else {
                    pendingIntentBackgroundActivityStartMode = mode
                }
            }.toBundle()
        }

        private fun broadcastPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent
        ) = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
