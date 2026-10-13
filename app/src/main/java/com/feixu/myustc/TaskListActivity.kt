package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class TaskListActivity : AppCompatActivity() {
    //任务清单页面
    private lateinit var recyclerView: RecyclerView
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task_list)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        // 根据主题设置Toolbar样式
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)

        recyclerView = findViewById(R.id.recycler_view)
        textView = findViewById(R.id.text_view)

        //设置下拉刷新
        val refreshLayout: SwipeRefreshLayout = findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            loadTask()
            refreshLayout.isRefreshing = false
        }

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "other") //加载背景图片
    }

    override fun onResume() {
        super.onResume()
        loadTask()
        handleWidgetIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recyclerView.post(::handleWidgetIntent)
    }

    private fun handleWidgetIntent() {
        if (intent.getBooleanExtra(EXTRA_ADD_TASK, false)) {
            intent.removeExtra(EXTRA_ADD_TASK)
            addTask(recyclerView)
            return
        }

        val taskIndex = intent.getIntExtra(EXTRA_EDIT_TASK_INDEX, -1)
        if (taskIndex >= 0) {
            intent.removeExtra(EXTRA_EDIT_TASK_INDEX)
            (recyclerView.adapter as? TasksAdapter)?.editTask(this, taskIndex)
        }
    }

    private fun loadTask() {
        //加载任务
        val tasks = TasksHelper.readTasks(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val tasksAdapter = TasksAdapter(tasks)
        tasksAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() = updateEmptyState(tasksAdapter.itemCount)

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateEmptyState(tasksAdapter.itemCount)

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateEmptyState(tasksAdapter.itemCount)
        })
        recyclerView.adapter = tasksAdapter
        updateEmptyState(tasksAdapter.itemCount)
    }

    private fun updateEmptyState(taskCount: Int) {
        if (taskCount == 0) {
            recyclerView.visibility = View.GONE
            textView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            textView.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    fun addTask(view: View) {
        //添加任务
        val editTaskView = LayoutInflater.from(this).inflate(R.layout.edit_task_view, null)
        val taskTimeView = editTaskView.findViewById<TextView>(R.id.time)
        taskTimeView.setOnClickListener { TaskDateTimePicker.show(this, taskTimeView) }
        ThemeHelper.createDialogBuilder(this).setView(editTaskView)
            .setPositiveButton("确定") { _, _ ->
                val taskTitle = editTaskView.findViewById<EditText>(R.id.title).text.toString() //任务标题
                val taskContent = editTaskView.findViewById<EditText>(R.id.content).text.toString() //任务内容
                val taskTime = taskTimeView.text.toString() //任务时间
                val taskColorGroup = editTaskView.findViewById<RadioGroup>(R.id.task_color_group)
                val taskColor = TaskColor.fromSelectorId(taskColorGroup.checkedRadioButtonId)
                val task = Task(taskTitle, taskContent, taskTime, false, taskColor)
                val tasks: MutableList<Task> = TasksHelper.readTasks(this)
                tasks.add(task)
                TasksHelper.saveTasks(this, tasks)
                (findViewById<RecyclerView>(R.id.recycler_view).adapter as TasksAdapter).addTask(task)
            }
            .setNegativeButton("取消") { _, _ -> }
            .setCancelable(false).show()
    }

    companion object {
        const val EXTRA_ADD_TASK = "com.feixu.myustc.extra.ADD_TASK"
        const val EXTRA_EDIT_TASK_INDEX = "com.feixu.myustc.extra.EDIT_TASK_INDEX"
    }
}
