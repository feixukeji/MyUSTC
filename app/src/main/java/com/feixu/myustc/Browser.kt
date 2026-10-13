package com.feixu.myustc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.MailTo
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import java.net.URLDecoder
import java.util.Locale
import androidx.core.view.isVisible


@SuppressLint("SetJavaScriptEnabled")
class Browser(
    private val mActivity: Activity,
    private val webView: WebView,
    private val progressBar: ProgressBar?,
    name: String?,
    private var url: String? = null,
    visible: Boolean = false,
    private val callback: (() -> Unit)? = null,
    private val callbackWhenLogin: (() -> Unit)? = null,
    private val onImportSuccess: ((Boolean) -> Unit)? = null,
    private val onImportFail: ((String) -> Unit)? = null,
    private val onLoginCancel: (() -> Unit)? = null
) { //用于网页浏览
    val tag: String? //跳转网址

    private val setOfGraduateStudent = setOf("BA", "BE", "BZ", "SA", "SB", "SM", "SF", "BJ", "BC", "SC", "BL", "SL", "CB", "CS") //研究生学号前缀

    //统一身份认证和科大邮箱账号密码
    private var username = ""
    private var password = ""
    private var emailUsername = ""
    private var emailPassword = ""

    private var pageFinished = false //页面加载是否完成
    private var haveFillPassword = false //密码是否自动填充过了
    private var startDateChanged = false

    private val setting = mActivity.getSharedPreferences("WebActivity", MODE_PRIVATE)

    private var dialog: AlertDialog.Builder? = null //待显示的对话框

    var visible = false
        set(value) {
            field = value
            if (value && dialog != null) {
                dialog?.show()
                dialog = null
            }
        }

    init {
        this.visible = visible
        if (url != null) {
            tag = null
        } else
            when (name) {
                "StudySystem" -> { //本科生教务系统
                    url = mActivity.getString(R.string.study_system_login_url)
                    tag = null
                }

                "CourseTable" -> { //本科生获取课程表
                    url = mActivity.getString(R.string.study_system_login_url)
                    tag = mActivity.getString(R.string.course_table_url)
                }

                "GetExamArrange" -> { //本科生获取考试信息
                    url = mActivity.getString(R.string.study_system_login_url)
                    tag = mActivity.getString(R.string.exam_arrange_url)
                }

                "GradeSheet" -> { //本科生我的成绩
                    url = mActivity.getString(R.string.study_system_login_url)
                    tag = mActivity.getString(R.string.grade_sheet_url)
                }

                "GraduateStudentPlatform" -> { //研究生综合服务平台
                    url = mActivity.getString(R.string.graduate_student_platform_login_url)
                    tag = null
                }

                "CourseList" -> { //研究生课程表
                    url = mActivity.getString(R.string.graduate_student_platform_login_url)
                    tag = mActivity.getString(R.string.myustc_page_url) + name + ".html"
                }

                "CourseAssignment" -> { //研究生课程作业
                    url = mActivity.getString(R.string.graduate_student_platform_login_url)
                    tag = mActivity.getString(R.string.myustc_page_url) + name + ".html"
                }

                "ScoreQuery" -> { //研究生我的成绩
                    url = mActivity.getString(R.string.graduate_student_platform_login_url)
                    tag = mActivity.getString(R.string.myustc_page_url) + name + ".html"
                }

                "NetworkLogin" -> { //不能使用重定向，因为此时用户还未连接到网络
                    url = "http://wlt.ustc.edu.cn/cgi-bin/ip"
                    tag = null
                }

                else -> {
                    url = mActivity.getString(R.string.myustc_page_url) + name + ".html"
                    tag = null
                }
            }

        @Suppress("DEPRECATION")
        webView.settings.let {
            if (name !in setOf("InnovationPracticeBase", "PhysicalExperimentTool", "NetworkLogin"))
                if (mActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
                //深色模式
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) it.isAlgorithmicDarkeningAllowed = true
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.forceDark = WebSettings.FORCE_DARK_ON
            it.javaScriptEnabled = true
            it.setSupportZoom(true)
            it.builtInZoomControls = true
            it.displayZoomControls = false
            it.allowFileAccess = true
            it.loadWithOverviewMode = true
            it.javaScriptCanOpenWindowsAutomatically = true
            it.databaseEnabled = true
            it.domStorageEnabled = true
            it.setGeolocationEnabled(true)
            it.useWideViewPort = true
        }

        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(object : Any() { //在evaluateJavascript中调用
            @JavascriptInterface
            fun toInputPassword() { //统一身份认证账号密码错误，去重新输入账号密码
                mActivity.runOnUiThread {
                    inputPassword(message = "学号或密码错误 请重新输入")
                }
            }

            @JavascriptInterface
            fun toInputEmailPassword() { //科大邮箱账号密码错误，去重新输入账号密码
                mActivity.runOnUiThread {
                    inputEmailPassword(message = "邮箱或密码错误 请重新输入")
                }
            }

            @JavascriptInterface
            fun importCourse(src: String) { //导入课程表数据
                try {
                    val trimmed = src.trim()
                    if (!trimmed.startsWith("{")) throw Exception()

                    // 在线课程表文件和自定义课程文件分开存放，只有在线数据变化时才覆盖。
                    val oldCourses = FileHelper.read(mActivity, "courses.json")
                    val coursesChanged = trimmed != oldCourses
                    if (coursesChanged) {
                        FileHelper.save(mActivity, "courses.json", trimmed)
                    }
                    val hasChanged = startDateChanged || coursesChanged

                    mActivity.runOnUiThread {
                        if (hasChanged) {
                            TodayCourseWidget.refreshAllWidgets(mActivity)
                        }
                        onImportSuccess?.invoke(hasChanged) ?: callback?.invoke()
                    }
                } catch (_: Exception) {
                    mActivity.runOnUiThread {
                        onImportFail?.invoke("课程表解析失败，请稍后重试")
                    }
                }
            }

            @JavascriptInterface
            fun importStartDate(startDate: String) { //导入本学期起始日期
                val normalized = startDate.trim().take(10)
                val semesterSettings = mActivity.getSharedPreferences("Semester", MODE_PRIVATE)
                val oldStartDate = semesterSettings.getString("StartDate", "")?.trim()?.take(10).orEmpty()
                if (normalized != oldStartDate) {
                    startDateChanged = true
                    semesterSettings.edit { putString("StartDate", normalized) }
                }
            }

            @JavascriptInterface
            fun importExam(examsStr: String) { //导入考试信息数据
                try {
                    val src = if (examsStr == "") {
                        ""
                    } else {
                        val examsData = examsStr.substring(0, examsStr.length - 1).split(';')
                        val exams: MutableList<Exam> = mutableListOf()
                        examsData.forEach { item ->
                            val examData = item.split(',')
                            val exam = Exam(examData[0], examData[1], examData[2])
                            exams.add(exam)
                        }
                        exams.sortBy { exam -> exam.time }
                        val data = StringBuilder()
                        exams.forEach { exam ->
                            data.append(exam.name).append(',').append(exam.classroom).append(',').append(exam.time).append(';')
                        }
                        data.substring(0, data.length - 1)
                    }
                    val tmp = FileHelper.read(mActivity, "Exam.fx")
                    val hasChanged = src != tmp
                    if (hasChanged) {
                        FileHelper.save(mActivity, "Exam.fx", src)
                        ExamInfoWidget.refreshAllWidgets(mActivity)
                    }
                    mActivity.runOnUiThread {
                        onImportSuccess?.invoke(hasChanged) ?: callback?.invoke()
                    }
                } catch (_: Exception) {
                    mActivity.runOnUiThread {
                        onImportFail?.invoke("考试信息解析失败，请稍后重试")
                    }
                }
            }

            @JavascriptInterface
            fun importCredit(credit: String) { //导入总学分
                val gradeOverview = mActivity.getSharedPreferences("GradeOverview", MODE_PRIVATE)
                val tmp = gradeOverview.getString("Credit", "")
                if (credit != tmp) {
                    gradeOverview.edit { putString("Credit", credit) }
                    if (tmp != "") callback?.invoke()
                }
            }

            @JavascriptInterface
            fun downloadBlobFile(base64Data: String, fileName: String, _mimeType: String) { //处理blob文件下载
                mActivity.runOnUiThread {
                    FileHelper.downloadBlob(mActivity, base64Data, fileName)
                }
            }
        }, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                try {
                    pageFinished = false
                    if (url?.startsWith(mActivity.getString(R.string.study_system_url)) == true
                        || url?.startsWith(mActivity.getString(R.string.graduate_student_platform_url) + "yjsemaphome") == true
                        || url?.startsWith(mActivity.getString(R.string.passport_success_url)) == true
                        || url == mActivity.getString(R.string.physical_experiment_select_url) + "index.php/web/index/index.html"
                    ) {
                        haveFillPassword = false

                        if (tag != null) { //进入教务系统后跳转到课程表或我的成绩等页面
                            view?.loadUrl(tag)
                            view?.clearHistory()
                        }

                        username = setting.getString("Username", "").toString().uppercase(Locale.getDefault())
                        password = Encrypt.encryptPassword(setting.getString("Password", "").toString())
                        callbackWhenLogin?.invoke()
                    }
                } catch (_: Exception) {
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return when {
                    request.url.toString().startsWith("mailto:") -> { //若链接是邮箱地址，则打开邮件应用
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>(MailTo.parse(request.url.toString()).to))
                        intent.type = "message/rfc822"
                        mActivity.startActivity(intent)
                        true
                    }

                    !request.url.toString().startsWith("http") -> { //打开第三方APP
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            mActivity.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(mActivity, "你试图打开的APP未安装", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    request.url.toString().startsWith("https://live.eeo.cn/") -> { //ClassIn
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        mActivity.startActivity(intent)
                        true
                    }

                    request.url.toString().startsWith("https://qm.qq.com/cgi-bin/qm/qr?k=am1BQdmIpeI4afX5jzv8kPreSB4cqFYN") -> { //我的科大QQ交流群
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, mActivity.getString(R.string.qq_group).toUri())
                            mActivity.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(mActivity, "无法启动QQ或TIM", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    else -> super.shouldOverrideUrlLoading(view, request)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (error?.errorCode == ERROR_HOST_LOOKUP
                    && (webView.url?.startsWith(mActivity.getString(R.string.myustc_page_url)) == true
                            || name != "CentralStudyRoom" && name != "Game")
                ) webView.loadUrl(mActivity.getString(R.string.error_page)) //网页无法访问
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                try {
                    if (newProgress < 100) {
                        progressBar?.visibility = if (webView.isVisible) View.VISIBLE else View.INVISIBLE
                        progressBar?.progress = newProgress
                    } else { //页面加载完成
                        progressBar?.visibility = View.INVISIBLE
                        if (!pageFinished) {
                            pageFinished = true
                            view.evaluateJavascript("html=document.body.innerHTML;html.includes('网页无法打开')&&html.includes('${view.url.toString()}')") { flag ->
                                if (flag == "true") view.loadUrl(mActivity.getString(R.string.error_page))
                            } //网页无法访问
                            when {
                                view.url?.startsWith(mActivity.getString(R.string.passport_login_url)) == true -> { //统一身份认证登录
                                    webView.evaluateJavascript(
                                        "obs2=new MutationObserver(function(mu){\n" +
                                                "    if(document.getElementsByClassName('ant-alert-message')[0]){\n" +
                                                "        obs2.disconnect();\n" +
                                                "        Android.toInputPassword();\n" +
                                                "    }\n" +
                                                "});\n" +
                                                "obs2.observe(document.body,{\n" +
                                                "    childList:true,\n" +
                                                "    subtree:true\n" +
                                                "});"
                                    ) {}
                                    //检测统一身份认证账号密码是否错误

                                    webView.evaluateJavascript(
                                        "obs=new MutationObserver(function(mu){\n" +
                                                "    check=document.getElementsByClassName('ant-checkbox-input')[0];\n" +
                                                "    if(check){\n" +
                                                "        obs.disconnect();\n" +
                                                "        check.click();\n" +
                                                "    }\n" +
                                                "});\n" +
                                                "obs.observe(document.body,{\n" +
                                                "    childList:true,\n" +
                                                "    subtree:true\n" +
                                                "});"
                                    ) {}
                                    //统一身份认证验证码勾选“信任此设备”

                                    username = setting.getString("Username", "").toString().uppercase(Locale.getDefault())
                                    password = Encrypt.encryptPassword(setting.getString("Password", "").toString())
                                    if (username == "") inputPassword() //首次登录要求输入账号密码
                                    else if (!haveFillPassword) fillPasswordAwait() //自动填充账号密码
                                }

                                view.url?.startsWith(mActivity.getString(R.string.study_system_url)) == true -> //教务系统首页
                                    view.evaluateJavascript("document.getElementsByName('viewport')[0].content='width=1300,initial-scale=0.2'") {}

                                view.url?.startsWith(mActivity.getString(R.string.graduate_student_platform_url)) == true -> //研究生综合服务平台
                                    view.evaluateJavascript("document.getElementsByName('viewport')[0].content='width=1300,initial-scale=0.2'") {}

                                view.url?.startsWith(mActivity.getString(R.string.course_table_url) + '/') == true -> { //本科生课程表
                                    view.evaluateJavascript(
                                        "timer = window.setInterval( () => {\n" +
                                                "    semesterElem = document.getElementsByClassName('item')[0];\n" +
                                                "    startDateElem = document.getElementById('startDate');\n" +
                                                "    id = location.pathname.split('/').filter(Boolean).pop();\n" +
                                                "    if (semesterElem && startDateElem && id) {\n" +
                                                "        window.clearInterval(timer);\n" +
                                                "        Android.importStartDate(startDateElem.textContent || '');\n" +
                                                "        fetch('/for-std/course-table/semester/' + semesterElem.dataset.value + '/print-data/' + id, {\n" +
                                                "            credentials: 'include'\n" +
                                                "        }).then(response => response.json()).then(data => {\n" +
                                                "            Android.importCourse(JSON.stringify(data));\n" +
                                                "        }).catch( () => {\n" +
                                                "            Android.importCourse('');\n" +
                                                "        });\n" +
                                                "    }\n" +
                                                "}\n" +
                                                ", 500);"
                                    ) {} //获取JSON课程表
                                }

                                view.url?.startsWith(mActivity.getString(R.string.exam_arrange_url) + '/') == true -> { //考试信息
                                    view.evaluateJavascript(
                                        "examsStr='';\n" +
                                                "timer=window.setInterval(()=>{\n" +
                                                "    examsElem=document.getElementById('exams');\n" +
                                                "    if(examsElem){\n" +
                                                "        window.clearInterval(timer);\n" +
                                                "        exams=examsElem.getElementsByTagName('tr');\n" +
                                                "        for(i=1;i<exams.length;++i){\n" +
                                                "            exam=exams[i].getElementsByTagName('td');\n" +
                                                "            examsStr=examsStr+exam[2].innerHTML+','+exam[4].innerHTML+','+exam[3].innerHTML+';';\n" +
                                                "        }\n" +
                                                "        Android.importExam(examsStr);\n" +
                                                "    }\n" +
                                                "},200);"
                                    ) {} //获取考试信息
                                }

                                view.url == mActivity.getString(R.string.grade_sheet_url) -> { //我的成绩
                                    view.evaluateJavascript("document.getElementsByName('viewport')[0].content='width=1000,initial-scale=0.52'") {}
                                    view.evaluateJavascript(
                                        "timer=window.setInterval(()=>{\n" +
                                                "    sp=document.getElementsByClassName('overview')[0].getElementsByTagName('span')[2];\n" +
                                                "    if(sp){\n" +
                                                "        credit=sp.innerHTML;\n" +
                                                "        if(credit!=''&&!isNaN(credit)){\n" +
                                                "            window.clearInterval(timer);\n" +
                                                "            Android.importCredit(credit);\n" +
                                                "        }\n" +
                                                "    }\n" +
                                                "},1000);"
                                    ) {} //获取总学分
                                }

                                view.url == mActivity.getString(R.string.blackboard_learn_url) -> { //BB教学平台
                                    if (this@Browser.visible) Toast.makeText(mActivity, "正在跳转...", Toast.LENGTH_SHORT).show()
                                    view.evaluateJavascript("document.getElementsByTagName('td')[1].getElementsByTagName('a')[0].click()") {}
                                }

                                view.url?.startsWith(mActivity.getString(R.string.blackboard_learn_url) + "nginx_auth/") == true -> //BB教学平台登录
                                    view.evaluateJavascript("document.getElementsByTagName('a')[0].click()") {}

                                view.url == mActivity.getString(R.string.ustc_email_login_url) -> { //科大邮箱登录
                                    webView.evaluateJavascript(
                                        "obs2=new MutationObserver(function(mu){\n" +
                                                "    if(['账号或密码错误，请重新输入','帳號或密碼錯誤，請重新輸入','username or password wrong'].includes(mu[0]['addedNodes'][0].textContent)){\n" +
                                                "        Android.toInputEmailPassword();\n" +
                                                "    }\n" +
                                                "});\n" +
                                                "obs2.observe(document.body,{\n" +
                                                "    childList:true,\n" +
                                                "    subtree:true\n" +
                                                "});"
                                    ) {}
                                    //检测科大邮箱账号密码是否错误

                                    emailUsername = setting.getString("EmailUsername", "").toString()
                                    emailPassword = Encrypt.encryptPassword(setting.getString("EmailPassword", "").toString())
                                    if (emailUsername == "") inputEmailPassword() //首次登录要求输入账号密码
                                    else if (!haveFillPassword) fillEmailPasswordAwait() //自动填充账号密码
                                }

                                view.url?.startsWith(mActivity.getString(R.string.physical_experiment_select_url)) == true -> //大物预约系统
                                    view.evaluateJavascript("login()") {}
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            //文件选择器
            override fun onShowFileChooser(webView: WebView?, valueCallback: ValueCallback<Array<Uri?>?>, fileChooserParams: FileChooserParams?): Boolean {
                try {
                    if (mActivity is WebActivity) {
                        var acceptType = "*/*"
                        if (fileChooserParams != null) {
                            val acceptTypes = fileChooserParams.acceptTypes
                            val sb = StringBuilder()
                            for (type in acceptTypes) sb.append(type).append(';')
                            if (sb.length >= 4) acceptType = sb.substring(0, sb.length - 1)
                        }
                        mActivity.uploadFileAboveL = valueCallback
                        mActivity.mStartForResult.launch(acceptType)
                    }
                } catch (_: Exception) {
                }
                return true
            }

            //精准定位服务
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                Toast.makeText(mActivity, "为保护你的隐私，本软件未申请定位权限", Toast.LENGTH_SHORT).show()
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

            //显示网页中的警告对话框
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                try {
                    dialog = ThemeHelper.createDialogBuilder(mActivity).setMessage(message)
                        .setPositiveButton("确定") { _, _ -> result.confirm() }
                        .setCancelable(false)
                    if (this@Browser.visible) {
                        dialog?.show()
                        dialog = null
                    }
                } catch (_: Exception) {
                }
                return true
            }

            //显示网页中的确认对话框
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                try {
                    dialog = ThemeHelper.createDialogBuilder(mActivity).setMessage(message)
                        .setPositiveButton("确定") { _, _ -> result.confirm() }
                        .setNegativeButton("取消") { _, _ -> result.cancel() }
                        .setCancelable(false)
                    if (this@Browser.visible) {
                        dialog?.show()
                        dialog = null
                    }
                } catch (_: Exception) {
                }
                return true
            }

            //显示网页中的文本输入对话框
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
                try {
                    //获得一个LayoutInflater对象factory,加载指定布局成相应对象
                    val promptView = LayoutInflater.from(mActivity).inflate(R.layout.prompt_view, null)
                    //设置text对应网页中的提示信息,edit设置来自于网页的默认文字
                    promptView.findViewById<TextView>(R.id.text).text = message
                    promptView.findViewById<EditText>(R.id.edit).setText(defaultValue)
                    //定义对话框上的确定按钮
                    dialog = ThemeHelper.createDialogBuilder(mActivity).setView(promptView)
                        .setPositiveButton("确定") { _, _ -> result.confirm(promptView.findViewById<EditText>(R.id.edit).text.toString()) }
                        .setNegativeButton("取消") { _, _ -> result.cancel() }
                        .setCancelable(false)
                    if (this@Browser.visible) {
                        dialog?.show()
                        dialog = null
                    }
                } catch (_: Exception) {
                }
                return true
            }
        }

        webView.setDownloadListener { fileURL, userAgent, contentDisposition, mimeType, contentLength ->
            //文件下载
            if (fileURL.startsWith("blob:")) {
                // 处理blob链接
                val fileName = URLUtil.guessFileName(fileURL, contentDisposition, mimeType).replace("[#%&{}\\\\<>*?/\\s$!'\":@+`|=]".toRegex(), "") //去除Android文件名不允许的字符
                dialog = ThemeHelper.createDialogBuilder(mActivity).setMessage("确认下载 $fileName？")
                    .setPositiveButton("确定") { _, _ ->
                        // 通过JavaScript获取blob数据并转换为base64
                        webView.evaluateJavascript(
                            """
                            (function() {
                                fetch('$fileURL')
                                    .then(response => response.blob())
                                    .then(blob => {
                                        const reader = new FileReader();
                                        reader.onload = function() {
                                            const base64Data = reader.result.split(',')[1];
                                            Android.downloadBlobFile(base64Data, '$fileName', '$mimeType');
                                        };
                                        reader.readAsDataURL(blob);
                                    })
                                    .catch(error => {
                                        console.error('Download blob failed:', error);
                                    });
                            })();
                        """.trimIndent()
                        ) { }
                    }
                    .setNegativeButton("取消") { _, _ -> }
                    .setCancelable(false)
            } else {
                // 处理普通链接
                val realMimeType = if (mimeType == "application/octet-stream" || mimeType == "application/oct-stream") null else mimeType //如果不改，文件类型可能会识别为.bin
                val decodedContentDisposition = URLDecoder.decode(contentDisposition, "UTF-8")
                val fileNamePos = decodedContentDisposition.indexOf("; filename*=UTF-8''")
                var fileName = if (fileNamePos == -1) URLUtil.guessFileName(URLDecoder.decode(fileURL, "UTF-8"), decodedContentDisposition, realMimeType)
                else decodedContentDisposition.substring(fileNamePos + 19) //获取文件名
                fileName = fileName.replace("[#%&{}\\\\<>*?/\\s$!'\":@+`|=]".toRegex(), "") //去除Android文件名不允许的字符
                dialog = ThemeHelper.createDialogBuilder(mActivity).setMessage("确认下载 $fileName？")
                    .setPositiveButton("确定") { _, _ -> FileHelper.download(mActivity, fileURL, fileName, realMimeType) }
                    .setNegativeButton("取消") { _, _ -> }
                    .setCancelable(false)
            }
            if (this@Browser.visible) {
                dialog?.show()
                dialog = null
            }
        }

        webView.loadUrl(url!!)
    }

    private fun inputPassword(message: String = "首次登录需输入密码 以后将自动登录") {
        //要求输入统一身份认证账号密码
        try {
            val loginView = LayoutInflater.from(mActivity).inflate(R.layout.login_view, null)
            loginView.findViewById<TextView>(R.id.tip).text = message
            loginView.findViewById<EditText>(R.id.username).setText(username)
            loginView.findViewById<EditText>(R.id.password).setText(password)
            dialog = ThemeHelper.createDialogBuilder(mActivity).setView(loginView)
                .setPositiveButton("确定") { _, _ ->
                    username = loginView.findViewById<EditText>(R.id.username).text.toString().uppercase(Locale.getDefault())
                    password = loginView.findViewById<EditText>(R.id.password).text.toString()
                    setting.edit { putString("Username", username) }
                    setting.edit { putString("Password", Encrypt.encryptPassword(password)) }

                    val settingPersonalize = mActivity.getSharedPreferences("Personalize", MODE_PRIVATE)
                    val graduateStudent = settingPersonalize.getBoolean("GraduateStudent", false) //研究生身份
                    val usernameType = if (username.length < 10) "" else username.substring(0, 2)
                    if (!graduateStudent && usernameType in setOfGraduateStudent) {
                        dialog = ThemeHelper.createDialogBuilder(mActivity).setMessage("当前为本科生模式，是否要切换为研究生模式？")
                            .setPositiveButton("是") { _, _ ->
                                settingPersonalize.edit { putBoolean("GraduateStudent", true) }
                                mActivity.getSharedPreferences("Semester", MODE_PRIVATE).edit { remove("StartDate") }
                                TodayCourseWidget.refreshAllWidgets(mActivity)
                                Toast.makeText(mActivity, "已切换为研究生模式", Toast.LENGTH_SHORT).show()
                                mActivity.finish()
                                val intent = Intent(mActivity.applicationContext, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                mActivity.startActivity(intent)
                            }
                            .setNegativeButton("否") { _, _ -> fillPassword() }
                            .setCancelable(false)
                        if (this@Browser.visible) {
                            dialog?.show()
                            dialog = null
                        }
                    } else fillPassword()
                }
                .setNegativeButton("取消") { _, _ -> onLoginCancel?.invoke() }
                .setNeutralButton("忘记密码") { _, _ -> webView.evaluateJavascript("document.getElementsByClassName('forgetPassword')[0].click();") {} }
                .setCancelable(false)
            if (this@Browser.visible) {
                dialog?.show()
                dialog = null
            }
        } catch (_: Exception) {
        }
    }

    private fun inputEmailPassword(message: String = "首次登录需输入密码 以后将自动登录") {
        //要求输入科大邮箱账号密码
        try {
            val emailLoginView = LayoutInflater.from(mActivity).inflate(R.layout.email_login_view, null)
            emailLoginView.findViewById<TextView>(R.id.tip).text = message
            emailLoginView.findViewById<EditText>(R.id.username).setText(if (emailUsername == "") "@mail.ustc.edu.cn" else emailUsername)
            emailLoginView.findViewById<EditText>(R.id.password).setText(emailPassword)
            dialog = ThemeHelper.createDialogBuilder(mActivity).setView(emailLoginView)
                .setPositiveButton("确定") { _, _ ->
                    emailUsername = emailLoginView.findViewById<EditText>(R.id.username).text.toString()
                    emailPassword = emailLoginView.findViewById<EditText>(R.id.password).text.toString()
                    setting.edit { putString("EmailUsername", emailUsername) }
                    setting.edit { putString("EmailPassword", Encrypt.encryptPassword(emailPassword)) }
                    fillEmailPassword()
                }
                .setNegativeButton("取消") { _, _ -> onLoginCancel?.invoke() }
                .setCancelable(false)
            if (this@Browser.visible) {
                dialog?.show()
                dialog = null
            }
        } catch (_: Exception) {
        }
    }

    private fun fillPassword() {
        //自动填充统一身份认证账号密码，登录
        try {
            haveFillPassword = true
            webView.evaluateJavascript(
                """
                eve=new Event('input',{bubbles:true});
                nameInput.value='$username';
                nameInput.dispatchEvent(eve);
                in2=document.querySelector('input[type="password"]');
                in2.value='$password';
                in2.dispatchEvent(eve);
                submitBtn.click();
            """.trimIndent()
            ) {}
        } catch (_: Exception) {
        }
    }

    private fun fillPasswordAwait() {
        //等待页面加载完成，自动填充统一身份认证账号密码，登录
        try {
            haveFillPassword = true
            webView.evaluateJavascript(
                """
                obs=new MutationObserver(function(mu){
                    if(document.getElementById('submitBtn')){
                        obs.disconnect();
                        setTimeout(() => {
                            eve=new Event('input',{bubbles:true});
                            nameInput.value='$username';
                            nameInput.dispatchEvent(eve);
                            in2=document.querySelector('input[type="password"]');
                            in2.value='$password';
                            in2.dispatchEvent(eve);
                            submitBtn.click();
                        }, 50);
                    }
                });
                obs.observe(document.body,{
                    childList:true,
                    subtree:true
                });
                
                obsTab=new MutationObserver(function(mu){
                    tabWrap=document.querySelector('.ant-tabs-nav-wrap');
                    if(tabWrap && !document.getElementById('myustc-autofill-hint')){
                        obsTab.disconnect();
                        hint=document.createElement('div');
                        hint.id='myustc-autofill-hint';
                        hint.style='font-size:12px;color:#666;border:1px solid #d9d9d9;padding:4px 8px;margin-bottom:8px;text-align:center;border-radius:4px;';
                        hint.textContent='我的科大自动填充';
                        tabWrap.parentNode.insertBefore(hint,tabWrap.nextSibling);
                    }
                });
                obsTab.observe(document.body,{
                    childList:true,
                    subtree:true
                });
            """.trimIndent()
            ) {}
        } catch (_: Exception) {
        }
    }

    private fun fillEmailPassword() {
        //自动填充科大邮箱账号密码，登录
        try {
            haveFillPassword = true
            webView.evaluateJavascript(
                """
                eve=new Event('input',{bubbles:true});
                in1=document.getElementsByClassName('account-input')[0];
                in1.value='$emailUsername';
                in1.dispatchEvent(eve);
                in2=document.getElementsByClassName('password')[0];
                in2.value='$emailPassword';
                in2.dispatchEvent(eve);
                anchor.click();
            """.trimIndent()
            ) {}
        } catch (_: Exception) {
        }
    }

    private fun fillEmailPasswordAwait() {
        //等待页面加载完成，自动填充科大邮箱账号密码，登录
        try {
            haveFillPassword = true
            webView.evaluateJavascript(
                """
                obs=new MutationObserver(function(mu){
                    btn=document.getElementById('anchor');
                    if(btn){
                        obs.disconnect();
                        eve=new Event('input',{bubbles:true});
                        in1=document.getElementsByClassName('account-input')[0];
                        in1.value='$emailUsername';
                        in1.dispatchEvent(eve);
                        in2=document.getElementsByClassName('password')[0];
                        in2.value='$emailPassword';
                        in2.dispatchEvent(eve);
                        btn.click();
                    }
                });
                obs.observe(document.body,{
                    childList:true,
                    subtree:true
                });
            """.trimIndent()
            ) {}
        } catch (_: Exception) {
        }
    }
}
