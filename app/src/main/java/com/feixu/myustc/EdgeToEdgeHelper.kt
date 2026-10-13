package com.feixu.myustc

import android.app.Activity
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isNotEmpty

object EdgeToEdgeHelper {

    private const val TOOLBAR_HEIGHT_DP = 44

    /**
     * 启用Edge-to-Edge并为Activity设置适当的insets处理
     */
    fun setupEdgeToEdge(
        activity: Activity,
        toolbar: Toolbar
    ) {
        enableEdgeToEdge(activity)
        setupToolbarInsets(activity, toolbar)
        setupNavigationBar(activity)
    }

    /**
     * 重载函数，为没有toolbar的Activity启用Edge-to-Edge并设置适当的insets处理
     */
    fun setupEdgeToEdge(activity: Activity) {
        enableEdgeToEdge(activity)
        setupNavigationBar(activity)
    }

    private fun enableEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    private fun setupToolbarInsets(activity: Activity, toolbar: Toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            adjustToolbarHeight(activity, view, systemBars.top)
            adjustToolbarPadding(view, systemBars.top)
            centerToolbarChildren(toolbar)

            insets
        }
    }

    private fun adjustToolbarHeight(activity: Activity, toolbar: View, statusBarHeight: Int) {
        val toolbarHeightPx = (TOOLBAR_HEIGHT_DP * activity.resources.displayMetrics.density).toInt()
        val totalHeight = statusBarHeight + toolbarHeightPx

        toolbar.layoutParams = toolbar.layoutParams.apply {
            height = totalHeight
        }
    }

    private fun adjustToolbarPadding(toolbar: View, statusBarHeight: Int) {
        toolbar.setPadding(0, statusBarHeight, 0, 0)
    }

    private fun centerToolbarChildren(toolbar: Toolbar) {
        toolbar.post {
            centerNavigationIcon(toolbar)
            centerOtherChildren(toolbar)
        }
    }

    private fun centerNavigationIcon(toolbar: Toolbar) {
        if (toolbar.isNotEmpty()) {
            val navigationView = toolbar.getChildAt(0)
            updateChildGravity(navigationView, Gravity.CENTER_VERTICAL)
        }
    }

    private fun centerOtherChildren(toolbar: Toolbar) {
        for (i in 1 until toolbar.childCount) {
            val childView = toolbar.getChildAt(i)
            val currentParams = childView.layoutParams as? Toolbar.LayoutParams
            currentParams?.let { params ->
                val horizontalGravity = params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                val newGravity = Gravity.CENTER_VERTICAL or horizontalGravity
                updateChildGravity(childView, newGravity)
            }
        }
    }

    private fun updateChildGravity(view: View, gravity: Int) {
        val layoutParams = view.layoutParams as? Toolbar.LayoutParams
        layoutParams?.let { params ->
            params.gravity = gravity
            view.layoutParams = params
        }
    }

    private fun setupNavigationBar(activity: Activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets.inset(0, 0, 0, systemBars.bottom)
        }
    }
}
