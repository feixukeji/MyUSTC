package com.feixu.myustc

data class Task(
    var title: String,
    var content: String,
    var time: String,
    var ok: Boolean,
    var color: TaskColor = TaskColor.NONE
)
//任务

enum class TaskColor(val colorResId: Int?, val selectorId: Int) {
    NONE(null, R.id.task_color_none),
    BLUE(R.color.task_color_blue, R.id.task_color_blue),
    GREEN(R.color.task_color_green, R.id.task_color_green),
    ORANGE(R.color.task_color_orange, R.id.task_color_orange),
    RED(R.color.task_color_red, R.id.task_color_red),
    PURPLE(R.color.task_color_purple, R.id.task_color_purple);

    companion object {
        fun fromStoredValue(value: String?): TaskColor =
            entries.firstOrNull { it.name == value } ?: NONE

        fun fromSelectorId(selectorId: Int): TaskColor =
            entries.firstOrNull { it.selectorId == selectorId } ?: NONE
    }
}
