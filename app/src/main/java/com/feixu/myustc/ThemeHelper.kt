package com.feixu.myustc

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.Menu
import android.view.View
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 主题工具类 - 用于应用焕新主题相关的样式
 */
object ThemeHelper {

    /**
     * 设置Toolbar样式，适配深色模式
     */
    fun applyToolbarStyle(context: Context, toolbar: Toolbar) {
        val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (context is Activity) {
            applyDialogStyle(context)
        }

        // 根据深色模式切换颜色
        try {
            if (isDarkMode) {
                // 深色模式：黑底白字
                toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.white))
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(context, R.color.white))
            } else {
                // 浅色模式：白底黑字
                toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.black))
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(context, R.color.black))
            }
        } catch (_: Exception) {
            // 忽略样式设置错误
        }
    }

    /**
     * 应用弹窗圆角样式
     */
    fun applyDialogStyle(activity: Activity) {
        activity.theme.applyStyle(R.style.ThemeOverlay_MyUSTC_MaterialAlertDialog, true)
    }

    /**
     * 获取对话框 Builder
     */
    fun createDialogBuilder(context: Context): AlertDialog.Builder {
        return MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_MyUSTC_MaterialAlertDialog)
    }

    /**
     * 设置Toolbar内的TextView和Button颜色，适配深色模式
     */
    fun applyToolbarChildrenStyle(context: Context, toolbar: Toolbar) {
        val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 根据深色模式切换颜色
        val textColor = if (isDarkMode) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.black)
        }

        for (i in 0 until toolbar.childCount) {
            when (val child = toolbar.getChildAt(i)) {
                is TextView -> child.setTextColor(textColor)
                is Button -> {
                    child.setTextColor(textColor)
                    // 设置Button灰色背景和大圆角
                    child.setBackgroundResource(R.drawable.toolbar_button_bg_fresh)
                    // 为Button的drawable图标设置tint
                    child.compoundDrawables.forEach { it?.setTint(textColor) }
                    child.compoundDrawablesRelative.forEach { it?.setTint(textColor) }
                }
            }
        }
    }

    fun applyMenuIconStyle(context: Context, menu: Menu?) {
        val iconColor = if (
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        ) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.black)
        }

        menu?.let {
            for (index in 0 until it.size) {
                it[index].icon?.setTint(iconColor)
            }
        }
    }

    fun applyStatusBarStyle(activity: Activity) {
        val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上使用新 API
            val controller = activity.window.insetsController
            if (controller != null) {
                if (!isDarkMode) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // Android 10 及以下使用旧 API
            val decorView = activity.window.decorView
            var flags = decorView.systemUiVisibility

            flags = if (!isDarkMode) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }

            decorView.systemUiVisibility = flags
        }
    }
}
