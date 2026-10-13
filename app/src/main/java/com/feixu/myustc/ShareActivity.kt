package com.feixu.myustc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_share)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // 使用EdgeToEdgeHelper设置edge-to-edge
        EdgeToEdgeHelper.setupEdgeToEdge(this, toolbar)

        // 根据主题设置Toolbar样式
        ThemeHelper.applyToolbarStyle(this, toolbar)
        ThemeHelper.applyToolbarChildrenStyle(this, toolbar)

        findViewById<Button>(R.id.share).setOnClickListener { share() }

        share()
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, "邀请使用“" + getString(R.string.app_name) + "”APP：" + getString(R.string.myustc_url))
        intent.type = "text/plain"
        startActivity(intent)
    }
}
