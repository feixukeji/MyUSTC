package com.feixu.myustc

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TasksHelper {
    //任务助手

    fun readTasks(mContext: Context): MutableList<Task> { //读任务
        val tasks: MutableList<Task> = mutableListOf()
        val data = FileHelper.read(mContext, "Task.fx")
        val tasksData = data.split('\\').filter { it.isNotEmpty() } //不同任务之间用“\“隔开
        tasksData.forEach { item ->
            val taskData = item.split('`') //一个任务的不同信息之间用“`“隔开
            if (taskData.size >= 4) {
                tasks.add(
                    Task(
                        taskData[0],
                        taskData[1],
                        taskData[2],
                        taskData[3] == "1",
                        TaskColor.fromStoredValue(taskData.getOrNull(4))
                    )
                )
            }
        }
        tasks.sortBy { task ->
            (if (task.ok) "1" else "0") + when {
                task.time == "" -> ""
                task.time.length <= 10 -> LocalDateTime.parse(task.time + " 0:00", DateTimeFormatter.ofPattern("yyyy-M-d H:mm"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                else -> LocalDateTime.parse(task.time, DateTimeFormatter.ofPattern("yyyy-M-d H:mm"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }
        } //按时间顺序排序
        // 过滤掉"点击添加任务"项
        tasks.removeAll { it.title == "点击添加任务" }
        return tasks
    }

    fun saveTasks(mContext: Context, tasks: List<Task>) { //写任务
        val data = StringBuilder()
        // 过滤掉"点击添加任务"项
        tasks.filter { it.title != "点击添加任务" }.forEach { task ->
            data.append(task.title.replace('\\', ' ').replace('`', ' ')).append('`')
                .append(task.content.replace('\\', ' ').replace('`', ' ')).append('`')
                .append(task.time).append('`').append((if (task.ok) '1' else '0')).append('`')
                .append(task.color.name).append('\\')
        }
        FileHelper.save(mContext, "Task.fx", if (data.isEmpty()) "" else data.substring(0, data.length - 1))
        TaskListWidget.refreshAllWidgets(mContext)
    }

    fun completeTask(context: Context, position: Int) {
        val tasks = readTasks(context)
        val task = tasks.getOrNull(position) ?: return
        if (task.ok) return
        task.ok = true
        saveTasks(context, tasks)
    }

    fun formatTaskTime(time: String): String {
        val date = runCatching {
            LocalDate.parse(time.substringBefore(' '), DateTimeFormatter.ofPattern("yyyy-M-d"))
        }.getOrNull() ?: return time
        val today = LocalDate.now()
        val dateText = when (date) {
            today -> "今天"
            today.plusDays(1) -> "明天"
            else -> if (date.year == today.year) {
                "${date.monthValue}-${date.dayOfMonth}"
            } else {
                "${date.year}-${date.monthValue}-${date.dayOfMonth}"
            }
        }
        return listOf(
            dateText,
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA),
            time.substringAfter(' ', "")
        ).filter(String::isNotEmpty).joinToString(" ")
    }
}
