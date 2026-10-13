package com.feixu.myustc

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    //首次使用当前版本出现的欢迎页面
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this)
    }

    fun agree(view: View) {
        finish()
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
