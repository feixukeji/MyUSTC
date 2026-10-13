package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar


class AboutActivity : AppCompatActivity() {
    //关于页面

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        // 根据主题设置Toolbar样式
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)

        val versionView = findViewById<TextView>(R.id.text_version)
        versionView.text = "Version " + BuildConfig.VERSION_NAME //显示当前版本名
    }

    fun update(view: View) {
        Update(this, true) //检查更新
    }

    fun certificate(view: View) {
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("text", "证书")
        intent.putExtra("name", "Certificate")
        startActivity(intent)
    }

    fun sxl(view: View) {
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("text", "关于开发者")
        intent.putExtra("name", "sxl")
        startActivity(intent)
    }

    fun serviceAgreement(view: View) {
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("text", "服务协议")
        intent.putExtra("name", "ServiceAgreement")
        startActivity(intent)
    }

    fun privacyPolicy(view: View) {
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("text", "隐私政策")
        intent.putExtra("name", "PrivacyPolicy")
        startActivity(intent)
    }

    fun beianQuery(view: View) {
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("text", "备案查询")
        intent.putExtra("name", "BeianQuery")
        startActivity(intent)
    }
}
