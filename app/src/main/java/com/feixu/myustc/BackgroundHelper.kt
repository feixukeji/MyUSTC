package com.feixu.myustc

import android.content.Context
import android.view.View
import android.widget.ImageView

object BackgroundHelper {
    fun loadBackground(mContext: Context, backgroundView: ImageView, type: String) {
        //加载背景图片
        val settingPersonalize = mContext.getSharedPreferences("Personalize", Context.MODE_PRIVATE)
        val backgroundMode = settingPersonalize.getString("BackgroundMode", "no")
        if (backgroundMode == "no" || backgroundMode == "main" && type == "other")
            backgroundView.visibility = View.GONE
        else {
            val image = FileHelper.readImage(mContext, "background.jpg")
            if (image == null) backgroundView.visibility = View.GONE
            else {
                backgroundView.setImageBitmap(image)
                backgroundView.alpha = settingPersonalize.getInt("BackgroundAlpha", 20) / 100f
                backgroundView.visibility = View.VISIBLE
            }
        }
    }
}