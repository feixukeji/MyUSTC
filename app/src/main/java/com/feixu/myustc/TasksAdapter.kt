package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class TasksAdapter(private val tasksList: MutableList<Task>, private val maxCount: Int? = null) :
    RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {
    //任务适配器

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val taskTitleView: TextView = itemView.findViewById(R.id.title)
        val taskTimeView: TextView = itemView.findViewById(R.id.time)
        val colorIndicator: View = itemView.findViewById(R.id.color_indicator)
        val taskView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasksList[position]
        holder.checkbox.isChecked = task.ok
        val colorResId = task.color.colorResId
        if (colorResId == null) {
            holder.colorIndicator.visibility = View.GONE
        } else {
            holder.colorIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.taskView.context, colorResId)
            )
            holder.colorIndicator.alpha = if (task.ok) 0.45f else 1f
            holder.colorIndicator.visibility = View.VISIBLE
        }
        if (task.ok) holder.taskTitleView.paintFlags = holder.taskTitleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else holder.taskTitleView.paintFlags = holder.taskTitleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        holder.checkbox.setOnClickListener {
            val clickedPosition = holder.bindingAdapterPosition
            if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            tasksList[clickedPosition].ok = holder.checkbox.isChecked
            notifyItemChanged(clickedPosition)
            TasksHelper.saveTasks(holder.taskView.context, tasksList)
        } //任务是否完成

        holder.taskTitleView.text = task.title //任务标题

        if (task.time == "") holder.taskTimeView.visibility = View.GONE
        else {
            holder.taskTimeView.text = TasksHelper.formatTaskTime(task.time)
            holder.taskTimeView.visibility = View.VISIBLE
        } //任务时间

        holder.taskView.setOnClickListener {
            val clickedPosition = holder.bindingAdapterPosition
            if (clickedPosition != RecyclerView.NO_POSITION) {
                editTask(holder.taskView.context, clickedPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (maxCount != null && maxCount < tasksList.size) maxCount else tasksList.size
    }

    fun addTask(task: Task) {
        //告知适配器添加任务
        tasksList.add(task)
        notifyItemInserted(tasksList.lastIndex)
    }

    @SuppressLint("SetTextI18n")
    fun editTask(context: Context, position: Int) {
        if (position !in tasksList.indices) return
        val task = tasksList[position]

        val editTaskView = LayoutInflater.from(context).inflate(R.layout.edit_task_view, null)
        val taskTitleView = editTaskView.findViewById<EditText>(R.id.title)
        val taskContentView = editTaskView.findViewById<EditText>(R.id.content)
        val taskTimeView = editTaskView.findViewById<TextView>(R.id.time)
        val taskColorGroup = editTaskView.findViewById<RadioGroup>(R.id.task_color_group)
        editTaskView.findViewById<TextView>(R.id.text).text = "任务详情"
        taskTitleView.setText(task.title)
        taskContentView.setText(task.content)
        taskTimeView.text = task.time
        taskColorGroup.check(task.color.selectorId)
        taskTimeView.setOnClickListener { TaskDateTimePicker.show(context, taskTimeView) }

        ThemeHelper.createDialogBuilder(context).setView(editTaskView)
            .setPositiveButton("确定") { _, _ ->
                task.title = taskTitleView.text.toString()
                task.content = taskContentView.text.toString()
                task.time = taskTimeView.text.toString()
                task.color = TaskColor.fromSelectorId(taskColorGroup.checkedRadioButtonId)
                notifyItemChanged(position)
                TasksHelper.saveTasks(context, tasksList)
            }
            .setNegativeButton("取消") { _, _ -> }
            .setNeutralButton("删除") { _, _ ->
                tasksList.removeAt(position)
                notifyItemRemoved(position)
                TasksHelper.saveTasks(context, tasksList)
            }
            .show()
    }
}
