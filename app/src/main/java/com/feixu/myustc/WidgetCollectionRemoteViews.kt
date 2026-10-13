package com.feixu.myustc

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Builds the collections displayed by the home-screen widgets. */
object WidgetCollectionRemoteViews {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun setTodayCourseAdapter(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        displayDate: LocalDate,
        courses: List<TodayCourseItem>
    ) {
        setRemoteAdapter(
            context,
            views,
            appWidgetId,
            R.id.widget_course_list,
            courses.map { createTodayCourseItemView(context, displayDate, it) }
        )
    }

    fun setExamAdapter(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        exams: List<ExamDisplayItem>
    ) {
        setRemoteAdapter(
            context,
            views,
            appWidgetId,
            R.id.widget_exam_list,
            exams.map { createExamItemView(context, it) }
        )
    }

    fun setTaskAdapter(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        tasks: List<IndexedValue<Task>>
    ) {
        setRemoteAdapter(
            context,
            views,
            appWidgetId,
            R.id.widget_task_list,
            tasks.map { createTaskItemView(context, it) }
        )
    }

    private fun setRemoteAdapter(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        viewId: Int,
        itemViews: List<RemoteViews>,
        viewTypeCount: Int = 1
    ) {
        val items = RemoteViewsCompat.RemoteCollectionItems.Builder()
            .setHasStableIds(false)
            .setViewTypeCount(viewTypeCount)
            .apply {
                itemViews.forEachIndexed { index, itemView ->
                    addItem(index.toLong(), itemView)
                }
            }
            .build()
        RemoteViewsCompat.setRemoteAdapter(context, views, appWidgetId, viewId, items)
    }

    private fun createTodayCourseItemView(
        context: Context,
        displayDate: LocalDate,
        item: TodayCourseItem
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_course_item)
        val (startTime, endTime) = parseClassTime(item.time)
        val now = LocalTime.now()
        val status = if (displayDate == LocalDate.now()) {
            calculateCourseStatus(startTime, endTime, now)
        } else {
            CourseStatus.NORMAL
        }

        views.setTextViewText(R.id.widget_course_name, item.name)
        views.setTextViewText(
            R.id.widget_course_teacher,
            TodayCourseDisplayFormatter.formatTeacherLine(item.teacher)
        )
        views.setTextViewText(
            R.id.widget_course_time_classroom,
            TodayCourseDisplayFormatter.formatWidgetTimeAndLocationLine(item.time, item.location)
        )
        views.setInt(
            R.id.widget_course_item,
            "setBackgroundResource",
            if (status == CourseStatus.UPCOMING || status == CourseStatus.ONGOING) {
                R.drawable.widget_course_highlight
            } else {
                0
            }
        )
        views.setViewVisibility(R.id.widget_countdown, View.GONE)
        views.setTextViewText(R.id.widget_countdown, "")
        views.setOnClickFillInIntent(R.id.widget_course_item, Intent())
        return views
    }

    private fun createExamItemView(context: Context, item: ExamDisplayItem): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_exam_item).apply {
            setTextViewText(R.id.widget_exam_countdown, formatExamCountdown(item))
            setTextViewText(R.id.widget_exam_name, item.name)
            setTextViewText(R.id.widget_exam_time, item.time)
            setTextViewText(R.id.widget_exam_classroom, item.classroom)
            setInt(R.id.widget_exam_item, "setBackgroundResource", 0)
            setOnClickFillInIntent(R.id.widget_exam_item, Intent())
        }
    }

    private fun createTaskItemView(
        context: Context,
        item: IndexedValue<Task>
    ): RemoteViews {
        val task = item.value
        return RemoteViews(context.packageName, R.layout.widget_task_item).apply {
            setTextViewText(R.id.widget_task_item_title, task.title)
            setTextViewText(R.id.widget_task_time, TasksHelper.formatTaskTime(task.time))
            setViewVisibility(
                R.id.widget_task_time,
                if (task.time.isBlank()) View.GONE else View.VISIBLE
            )
            setViewVisibility(
                R.id.widget_task_color,
                if (task.color.colorResId == null) View.INVISIBLE else View.VISIBLE
            )
            task.color.colorResId?.let {
                setInt(R.id.widget_task_color, "setBackgroundResource", it)
            }
            setOnClickFillInIntent(
                R.id.widget_task_details,
                Intent().putExtra(TaskListActivity.EXTRA_EDIT_TASK_INDEX, item.index)
            )
            setOnClickFillInIntent(
                R.id.widget_task_checkbox,
                Intent().putExtra(TaskListWidget.EXTRA_COMPLETE_TASK_POSITION, item.index)
            )
        }
    }

    private fun parseClassTime(time: String): Pair<LocalTime, LocalTime> {
        return try {
            val times = time.split(" - ")
            LocalTime.parse(times[0], timeFormatter) to
                LocalTime.parse(times[1], timeFormatter)
        } catch (_: Exception) {
            LocalTime.MIN to LocalTime.MAX
        }
    }

    private fun calculateCourseStatus(
        startTime: LocalTime,
        endTime: LocalTime,
        now: LocalTime
    ): CourseStatus {
        val minutesToStart = ChronoUnit.MINUTES.between(now, startTime)
        return when {
            now.isAfter(startTime) && now.isBefore(endTime) -> CourseStatus.ONGOING
            minutesToStart in 0..20 -> CourseStatus.UPCOMING
            else -> CourseStatus.NORMAL
        }
    }

    private fun formatExamCountdown(item: ExamDisplayItem): CharSequence {
        val examDate = item.startTime?.toLocalDate() ?: return "--"
        return when (val days = ChronoUnit.DAYS.between(LocalDate.now(), examDate).coerceAtLeast(0)) {
            0L -> "今天"
            1L -> "明天"
            else -> {
                val text = "$days 天"
                SpannableString(text).apply {
                    setSpan(
                        RelativeSizeSpan(0.7f),
                        text.lastIndex,
                        text.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }
}

enum class CourseStatus {
    NORMAL,
    UPCOMING,
    ONGOING
}
