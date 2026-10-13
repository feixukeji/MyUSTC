package com.feixu.myustc

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.feixu.myustc.ui.one.OneFragment
import com.feixu.myustc.ui.three.ThreeFragment
import com.feixu.myustc.ui.two.TwoFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.Integer.min
import java.util.Locale


class MainActivity : AppCompatActivity() {
    //主页面

    var viewPager: ViewPager2? = null //Fragment ViewPager
    var navView: BottomNavigationView? = null //导航栏

    private val setOfGraduateStudent = setOf("BA", "BE", "BZ", "SA", "SB", "SM", "SF", "BJ", "BC", "SC", "BL", "SL", "CB", "CS") //研究生学号前缀

    private var btnChooseBackground: Button? = null
    private var backgroundUri: Uri? = null

    @SuppressLint("SetTextI18n")
    private val mStartForResult = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
        if (result != null) {
            btnChooseBackground?.text = "重新选择"
            backgroundUri = result
        }
    } //背景图片选取，需要在onCreate之前register

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Use splash theme only for process launch, then switch to the regular app theme.
        setTheme(R.style.Theme_MyUSTC)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 处理底部导航栏
        navView = findViewById(R.id.nav_view)

        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)

        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        val switchIdentityButton = findViewById<Button>(R.id.switch_identity)

        when (settingPersonalize.getString("DarkMode", "auto")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } //深色模式

        if (settingPersonalize.getBoolean("GraduateStudent", false))
            switchIdentityButton.text = "研" //研究生模式

        //绑定ViewPager2与BottomNavigationView
        viewPager = findViewById(R.id.view_pager)
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                navView?.menu?.get(position)?.isChecked = true
            }
        })
        navView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_one -> viewPager?.currentItem = 0
                R.id.navigation_two -> viewPager?.currentItem = 1
                R.id.navigation_three -> viewPager?.currentItem = 2
            }
            true
        }
        viewPager?.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> OneFragment()
                    1 -> TwoFragment()
                    2 -> ThreeFragment()
                    else -> throw IndexOutOfBoundsException()
                }
            }
        }

        val setting = getSharedPreferences("Version", MODE_PRIVATE)
        val lastVersionName = setting.getString("VersionName", "")
        if (lastVersionName != BuildConfig.VERSION_NAME) {
            setting.edit { putString("VersionName", BuildConfig.VERSION_NAME) }

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel("1", "提醒", NotificationManager.IMPORTANCE_HIGH)
            ) //设置通知渠道

            startActivity(Intent(this, WelcomeActivity::class.java)) //首次使用当前版本则出现欢迎页面
        }
        val autoSync = settingPersonalize.getBoolean("AutoSync", true)
        if (autoSync) {
            val graduateStudent = settingPersonalize.getBoolean("GraduateStudent", false) //研究生身份
            if (graduateStudent)
                Browser(this, WebView(this), null, "GraduateStudentPlatform", visible = false) //预加载研究生综合服务平台
            else
                Browser(
                    this, WebView(this), null, "GetExamArrange", visible = false,
                    callbackWhenLogin = {
                        Browser(
                            this, WebView(this), null, "GradeSheet", getString(R.string.grade_sheet_url), visible = false,
                            callback = { //检查成绩单是否有更新
                                val tipCard = findViewById<CardView>(R.id.tip_card)
                                val tipView = findViewById<TextView>(R.id.tip)
                                tipView?.text = "成绩单有更新，点击查看>>"
                                tipView?.setOnClickListener {
                                    val intent = Intent(this, WebActivity::class.java)
                                    intent.putExtra("text", "我的成绩")
                                    intent.putExtra("name", "GradeSheet")
                                    startActivity(intent)
                                    tipCard?.visibility = View.GONE
                                }
                                findViewById<ImageView>(R.id.close)?.setOnClickListener {
                                    tipCard?.visibility = View.GONE
                                }
                                tipCard?.visibility = View.VISIBLE
                            }
                        )
                    })
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //按返回键时返回系统桌面，不要退出应用
                val home = Intent(Intent.ACTION_MAIN)
                home.addCategory(Intent.CATEGORY_HOME)
                startActivity(home)
            }
        })

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "main") //加载背景图片

        viewPager?.post { //在界面绘制完成后执行操作
            viewPager?.offscreenPageLimit = 2 //加载另外的Fragment
        }

        Update(this, false) //检查更新
    }

    fun selectIdentity(view: View) {
        //本科生、研究生身份选择
        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
        ThemeHelper.createDialogBuilder(this).setMessage("请选择身份")
            .setPositiveButton("本科生") { _, _ ->
                if (settingPersonalize.getBoolean("GraduateStudent", false)) changeIdentity(false)
            }
            .setNegativeButton("研究生") { _, _ ->
                if (!settingPersonalize.getBoolean("GraduateStudent", false)) changeIdentity(true)
            }
            .show()
    }

    private fun changeIdentity(graduateStudent: Boolean) {
        //本科生、研究生身份切换
        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
        settingPersonalize.edit { putBoolean("GraduateStudent", graduateStudent) }
        val settingWebActivity = getSharedPreferences("WebActivity", MODE_PRIVATE)
        val username = settingWebActivity.getString("Username", "").toString().uppercase(Locale.getDefault())
        val usernameType = if (username.length < 10) "" else username.substring(0, 2)
        if (graduateStudent && usernameType in setOf("PB", "JL", "PL") || !graduateStudent && usernameType in setOfGraduateStudent) {
            settingWebActivity.edit { remove("Username").remove("Password") }
            getSharedPreferences("Semester", MODE_PRIVATE).edit { remove("StartDate") }
            TodayCourseWidget.refreshAllWidgets(this)
        }
        if (graduateStudent) {
            baseContext.getFileStreamPath("courses.json").delete()
            baseContext.getFileStreamPath("Exam.fx").delete()
        }
        Toast.makeText(this, if (graduateStudent) "已切换为研究生模式" else "已切换为本科生模式", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    @SuppressLint("SetTextI18n")
    fun personalize() {
        //个性化
        val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
        val personalizeView = LayoutInflater.from(this).inflate(R.layout.personalize_view, null)

        val autoSyncTextView = personalizeView.findViewById<TextView>(R.id.text_auto_sync)
        autoSyncTextView.setOnClickListener {
            val massageView = LayoutInflater.from(this).inflate(R.layout.massage_view, null)
            massageView.findViewById<TextView>(R.id.text_title).text = "自动同步（试验性）"
            massageView.findViewById<TextView>(R.id.text_massage).text = "开启后可在启动应用时自动同步考试信息和成绩，在课程表页面自动同步课程表，在考试信息页面自动同步考试信息，若导致频繁收到短信验证码，请关闭本功能。"
            ThemeHelper.createDialogBuilder(this).setView(massageView)
                .setPositiveButton("确定") { _, _ -> }.show()
        }
        if (settingPersonalize.getBoolean("AutoSync", true))
            personalizeView.findViewById<RadioButton>(R.id.radio_enable_auto_sync).isChecked = true
        else personalizeView.findViewById<RadioButton>(R.id.radio_disable_auto_sync).isChecked = true //自动同步

        val punchRecord = getSharedPreferences("Punch", MODE_PRIVATE)
        if (punchRecord.getBoolean("Enable", true))
            personalizeView.findViewById<RadioButton>(R.id.radio_enable_punch).isChecked = true
        else personalizeView.findViewById<RadioButton>(R.id.radio_disable_punch).isChecked = true //每日签到

        when (settingPersonalize.getString("DarkMode", "auto")) {
            "light" -> personalizeView.findViewById<RadioButton>(R.id.radio_light).isChecked = true
            "dark" -> personalizeView.findViewById<RadioButton>(R.id.radio_dark).isChecked = true
            else -> personalizeView.findViewById<RadioButton>(R.id.radio_auto).isChecked = true
        } //深色模式

        var backgroundMode = settingPersonalize.getString("BackgroundMode", "no")
        when (backgroundMode) {
            "all" -> personalizeView.findViewById<RadioButton>(R.id.radio_background_all).isChecked = true
            "main" -> personalizeView.findViewById<RadioButton>(R.id.radio_background_main).isChecked = true
            else -> personalizeView.findViewById<RadioButton>(R.id.radio_background_no).isChecked = true
        }
        personalizeView.findViewById<LinearLayout>(R.id.background_option).visibility = if (backgroundMode == "no") View.GONE else View.VISIBLE
        val groupBackground = personalizeView.findViewById<RadioGroup>(R.id.group_background)
        groupBackground.setOnCheckedChangeListener { _, checkedId ->
            backgroundMode = when (checkedId) {
                R.id.radio_background_all -> "all"
                R.id.radio_background_main -> "main"
                else -> "no"
            }
            personalizeView.findViewById<LinearLayout>(R.id.background_option).visibility = if (backgroundMode == "no") View.GONE else View.VISIBLE
        }

        var backgroundAlpha = settingPersonalize.getInt("BackgroundAlpha", 20)
        val textBackgroundAlpha: TextView = personalizeView.findViewById(R.id.background_alpha)
        textBackgroundAlpha.text = "不透明度：$backgroundAlpha%"
        val barBackgroundAlpha: SeekBar = personalizeView.findViewById(R.id.background_alpha_bar)
        barBackgroundAlpha.progress = backgroundAlpha
        barBackgroundAlpha.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                backgroundAlpha = progress
                textBackgroundAlpha.text = "不透明度：$backgroundAlpha%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnChooseBackground = personalizeView.findViewById(R.id.choose_background)
        if (baseContext.getFileStreamPath("background.jpg").exists()) {
            btnChooseBackground?.text = "重新选择"
        }
        btnChooseBackground?.setOnClickListener {
            mStartForResult.launch("image/*")
        }

        val containerLayout = findViewById<ConstraintLayout>(R.id.container)
        personalizeView.findViewById<TextView>(R.id.choose_background_tip).text = "建议尺寸：" + containerLayout.width + "×" + containerLayout.height
        //背景图片

        ThemeHelper.createDialogBuilder(this).setView(personalizeView)
            .setPositiveButton("确定") { _, _ ->
                val checkedAutoSync = personalizeView.findViewById<RadioGroup>(R.id.group_auto_sync).checkedRadioButtonId == R.id.radio_enable_auto_sync
                settingPersonalize.edit(commit = true) { putBoolean("AutoSync", checkedAutoSync) } //自动同步

                val checkedPunch = personalizeView.findViewById<RadioGroup>(R.id.group_punch).checkedRadioButtonId == R.id.radio_enable_punch
                punchRecord.edit(commit = true) { putBoolean("Enable", checkedPunch) } //每日签到

                val checkedDarkMode = when (personalizeView.findViewById<RadioGroup>(R.id.group_dark_mode).checkedRadioButtonId) {
                    R.id.radio_light -> "light"
                    R.id.radio_dark -> "dark"
                    else -> "auto"
                }
                settingPersonalize.edit(commit = true) { putString("DarkMode", checkedDarkMode) } //深色模式

                settingPersonalize.edit(commit = true) { putString("BackgroundMode", backgroundMode) }
                settingPersonalize.edit(commit = true) { putInt("BackgroundAlpha", backgroundAlpha) }
                if (backgroundUri != null) {
                    val toast = Toast.makeText(this, "请稍后", Toast.LENGTH_SHORT)
                    toast.show()
                    val image = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, backgroundUri!!))
                    FileHelper.saveImage(this, "background.jpg", image.scale(min(containerLayout.width / 2, image.width), min(containerLayout.height / 2, image.height)))
                    backgroundUri = null
                    toast.cancel()
                }

                Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show()
                finish()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .setNegativeButton("取消") { _, _ -> }
            .setCancelable(false).show()
    }
}
