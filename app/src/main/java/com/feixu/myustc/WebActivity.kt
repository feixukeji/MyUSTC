package com.feixu.myustc

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors


class WebActivity : AppCompatActivity() {
    //网页浏览页面

    private var switchWeb1: Button? = null
    private var switchWeb2: Button? = null
    private var switchContainer: View? = null
    private var switchIndicator: View? = null
    private var webView: WebView? = null
    private var browser: Browser? = null
    private var webView2: WebView? = null
    private var browser2: Browser? = null
    private var nowWebView: WebView? = null //当前webView
    private var nowBrowser: Browser? = null //当前browser

    var uploadFileAboveL: ValueCallback<Array<Uri?>?>? = null
    val mStartForResult = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
        if (result == null) uploadFileAboveL?.onReceiveValue(null)
        else uploadFileAboveL?.onReceiveValue(arrayOf(result))
    } //文件选取，需要在onCreate之前register

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web)

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

        switchWeb1 = findViewById(R.id.switch_web1)
        switchWeb2 = findViewById(R.id.switch_web2)
        switchContainer = findViewById(R.id.switch_container)
        switchIndicator = findViewById(R.id.switch_indicator)
        webView = findViewById(R.id.web_view)
        webView2 = findViewById(R.id.web_view2)
        nowWebView = webView

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { //按返回键
                if (nowBrowser?.tag != null) finish()
                else
                    if (nowWebView?.url == getString(R.string.error_page)) {
                        nowWebView?.goBack()
                        if (nowWebView?.canGoBack() == true) nowWebView?.goBack()
                        else finish()
                    } else
                        if (nowWebView?.canGoBack() == true) nowWebView?.goBack()
                        else finish()
            }
        })

        //接收传递过来的功能名称、网址
        val bundle: Bundle? = intent.extras
        var text: String = bundle?.getString("text")!!
        var name: String? = bundle.getString("name")
        val url: String? = bundle.getString("url")

        if (name == "StudySystem") {
            val settingPersonalize = getSharedPreferences("Personalize", MODE_PRIVATE)
            val graduateStudent = settingPersonalize.getBoolean("GraduateStudent", false) //研究生身份
            if (graduateStudent) { //研究生综合服务平台
                text = "综合服务"
                name = "GraduateStudentPlatform"
            }
        }

        val titleView: TextView = findViewById(R.id.title)

        browser = Browser(this, webView!!, findViewById(R.id.progress_bar), name, url, visible = true)
        nowBrowser = browser
        if (name in setOf("StudySystem", "BlackboardLearn", "Vista", "PhysicalExperiment", "CourseResource", "EvaluateCourse", "SchoolHospital", "AccommodationService", "NewStudent", "TeachingAffair", "EnterLeave", "CampusScenery", "GradeSheet", "ScoreQuery")) {
            titleView.visibility = View.GONE
            switchWeb1?.visibility = View.VISIBLE
            switchWeb2?.visibility = View.VISIBLE
            switchContainer?.visibility = View.VISIBLE
            switchWeb1?.text = text
            when (name) {
                "StudySystem" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "EvaluateCourse", visible = false)
                    switchWeb2?.text = "评课社区"
                }

                "GradeSheet", "ScoreQuery" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "ScoreConversion", visible = false)
                    switchWeb2?.text = "分数换算"
                }

                "BlackboardLearn" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "HanhaiTeaching", visible = false)
                    switchWeb1?.text = "BB教学"
                    switchWeb2?.text = "瀚海教学"
                }

                "Vista" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "UstcFlyer", visible = false)
                    switchWeb2?.text = "飞跃网站"
                }

                "PhysicalExperiment" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "PhysicalExperimentTool", visible = false)
                    switchWeb1?.text = "大物预约"
                    switchWeb2?.text = "大物工具"
                }

                "CourseResource" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "ComputerBasedTest", visible = false)
                    switchWeb2?.text = "题库自测"
                }

                "EvaluateCourse" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "CourseSchedule", visible = false)
                    switchWeb1?.text = "评课社区"
                    switchWeb2?.text = "排课工具"
                }

                "SchoolHospital" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "MedicalInsurance", visible = false)
                    switchWeb2?.text = "医保系统"
                }

                "AccommodationService" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "HuazhuTravel", visible = false)
                    switchWeb1?.text = "宿舍服务"
                    switchWeb2?.text = "华住商旅"
                }

                "NewStudent" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "LifeStrategy", visible = false)
                    switchWeb1?.text = "迎新网"
                    switchWeb2?.text = "科大攻略"
                }

                "TeachingAffair" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "SecretaryInformation", visible = false)
                    switchWeb2?.text = "教秘信息"
                }

                "EnterLeave" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "FriendCome", visible = false)
                    switchWeb1?.text = "离返校"
                    switchWeb2?.text = "亲友入校"
                }

                "CampusScenery" -> {
                    browser2 = Browser(this, webView2!!, findViewById(R.id.progress_bar2), "CampusPanorama", visible = false)
                    switchWeb2?.text = "校园全景"
                }
            }
            switchToWeb1()
        } else
            titleView.text = text

        BackgroundHelper.loadBackground(this, findViewById(R.id.background), "other") //加载背景图片
    }

    fun switchToWeb1(view: View? = null) {
        switchWeb1?.isClickable = false
        switchWeb2?.isClickable = true
        updateSwitchButtonsStyle(isFirstSelected = true)
        findViewById<ConstraintLayout>(R.id.tab1).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.tab2).visibility = View.INVISIBLE
        browser?.visible = true
        browser2?.visible = false
        nowWebView = webView
        nowBrowser = browser
    }

    fun switchToWeb2(view: View? = null) {
        switchWeb1?.isClickable = true
        switchWeb2?.isClickable = false
        updateSwitchButtonsStyle(isFirstSelected = false)
        findViewById<ConstraintLayout>(R.id.tab1).visibility = View.INVISIBLE
        findViewById<ConstraintLayout>(R.id.tab2).visibility = View.VISIBLE
        browser?.visible = false
        browser2?.visible = true
        nowWebView = webView2
        nowBrowser = browser2
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.web_menu, menu)
        ThemeHelper.applyMenuIconStyle(this, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> //刷新页面
                if (nowWebView?.url == getString(R.string.error_page)) nowWebView?.goBack()
                else nowWebView?.reload()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSwitchButtonsStyle(isFirstSelected: Boolean) {
        val onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(this, R.color.black))
        val onSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, onSurface)

        switchWeb1?.backgroundTintList = null
        switchWeb2?.backgroundTintList = null
        switchWeb1?.background = null
        switchWeb2?.background = null

        if (isFirstSelected) {
            switchWeb1?.setTextColor(onSurface)
            switchWeb2?.setTextColor(onSurfaceVariant)
        } else {
            switchWeb1?.setTextColor(onSurfaceVariant)
            switchWeb2?.setTextColor(onSurface)
        }
        updateSwitchIndicator(isFirstSelected)
    }

    private fun updateSwitchIndicator(isFirstSelected: Boolean) {
        val indicator = switchIndicator ?: return
        val target = if (isFirstSelected) switchWeb1 else switchWeb2
        target?.post {
            val indicatorParams = indicator.layoutParams
            indicatorParams.width = target.width
            indicator.layoutParams = indicatorParams
            indicator.requestLayout()
            val parentView = target.parent as? View
            val targetX = (parentView?.x ?: 0f) + target.x
            val canAnimate = indicator.tag == true
            indicator.animate().cancel()
            if (canAnimate) {
                indicator.animate()
                    .x(targetX)
                    .setDuration(180)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                indicator.x = targetX
                indicator.tag = true
            }
            indicator.visibility = View.VISIBLE
        }
    }
}
