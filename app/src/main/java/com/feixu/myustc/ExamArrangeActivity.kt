package com.feixu.myustc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


class ExamArrangeActivity : AppCompatActivity() {
    //考试信息页面
    private lateinit var embeddedSync: StudySystemEmbeddedSync
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getPreferences(MODE_PRIVATE).edit {
                remove(NOTIFICATION_PERMISSION_REQUEST_COUNT)
            }
        } else {
            Toast.makeText(this, "未授予通知权限，考试提醒将无法显示", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_exam_arrange)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        // 根据主题设置Toolbar样式
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)

        // 设置状态栏图标颜色
        ThemeHelper.applyStatusBarStyle(this)

        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
        val autoSync = settingPersonalize.getBoolean("AutoSync", true)
        embeddedSync = StudySystemEmbeddedSync.examArrange(
            this,
            findViewById(R.id.sync_web_view),
            findViewById(R.id.progress_bar)
        )

        //设置下拉刷新
        val refreshLayout: SwipeRefreshLayout = findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            loadExam()
            if (autoSync) syncExamArrange(showDialog = false)
            refreshLayout.isRefreshing = false
        }

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "other") //加载背景图片

        if (autoSync) {
            syncExamArrange(showDialog = false)
        }
    }

    override fun onResume() {
        super.onResume()
        loadExam()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.exam_arrange_menu, menu)
        ThemeHelper.applyMenuIconStyle(this, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_exam -> { //获取教务系统考试信息
                syncExamArrange(showDialog = true)
            }

            R.id.exam_reminder -> { //设置考试提醒
                val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
                val remindBefore = settingPersonalize.getInt("RemindBefore", 0)
                val examReminderView = LayoutInflater.from(this).inflate(R.layout.exam_reminder_view, null)
                val remindBeforeView: EditText = examReminderView.findViewById(R.id.remind_before)
                if (remindBefore != 0) remindBeforeView.setText(remindBefore.toString())
                val dialog = ThemeHelper.createDialogBuilder(this).setView(examReminderView)
                    .setPositiveButton("确定") { _, _ -> }
                    .setNegativeButton("取消") { _, _ -> }
                    .setNeutralButton("不提醒") { _, _ ->
                        settingPersonalize.edit { putInt("RemindBefore", 0) }
                    }
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener {
                        try {
                            val remindBeforeNew = remindBeforeView.text.toString().toInt()
                            if (remindBeforeNew !in 1..1000) throw Exception()
                            settingPersonalize.edit { putInt("RemindBefore", remindBeforeNew) }
                            loadExam()
                            ensureNotificationPermission()
                            dialog.dismiss()
                        } catch (_: Exception) {
                            ThemeHelper.createDialogBuilder(this)
                                .setMessage("请填入 1 ~ 1000 的整数")
                                .setPositiveButton("确定") { _, _ -> }
                                .show()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun ensureNotificationPermission() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionWithRationale()
            return
        }

        if (!notificationManager.areNotificationsEnabled()) {
            openNotificationSettings()
            return
        }

        val channel = notificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
        if (channel == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "提醒",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        } else if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
            openNotificationChannelSettings()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionWithRationale() {
        val requestCount = getPreferences(MODE_PRIVATE).getInt(
            NOTIFICATION_PERMISSION_REQUEST_COUNT,
            0
        )
        when {
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                ThemeHelper.createDialogBuilder(this)
                    .setMessage("考试提醒需要通知权限才能正常显示")
                    .setPositiveButton("授权") { _, _ -> requestNotificationPermission() }
                    .setNegativeButton("取消") { _, _ -> }
                    .show()
            }

            requestCount < MAX_NOTIFICATION_PERMISSION_REQUESTS -> requestNotificationPermission()
            else -> openNotificationSettings()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val preferences = getPreferences(MODE_PRIVATE)
        val requestCount = preferences.getInt(NOTIFICATION_PERMISSION_REQUEST_COUNT, 0)
        preferences.edit {
            putInt(NOTIFICATION_PERMISSION_REQUEST_COUNT, requestCount + 1)
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        Toast.makeText(this, "请打开通知权限", Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        )
    }

    private fun openNotificationChannelSettings() {
        Toast.makeText(this, "请打开考试提醒通知渠道", Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, REMINDER_CHANNEL_ID)
            }
        )
    }

    private fun syncExamArrange(showDialog: Boolean) {
        embeddedSync.sync(
            showDialog = showDialog,
            onSuccess = { loadExam() }
        )
    }

    private fun loadExam() {
        //加载考试信息
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val textView: TextView = findViewById(R.id.text_view)
        try {
            textView.visibility = View.GONE

            val exams = ExamHelper.getUpcomingExams(this)
            ExamHelper.scheduleReminders(this, exams)
            if (exams.isNotEmpty()) {
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = ExamsAdapter(exams)
                recyclerView.visibility = View.VISIBLE
            } else textView.visibility = View.VISIBLE
        } catch (_: Exception) {
            recyclerView.visibility = View.GONE
            textView.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val REMINDER_CHANNEL_ID = "1"
        private const val NOTIFICATION_PERMISSION_REQUEST_COUNT = "notification_permission_request_count"
        private const val MAX_NOTIFICATION_PERMISSION_REQUESTS = 2
    }
}
