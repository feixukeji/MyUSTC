package com.feixu.myustc

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible

class StudySystemEmbeddedSync private constructor(
    private val activity: Activity,
    private val webView: WebView,
    private val progressBar: ProgressBar?,
    private val name: String,
    private val defaultErrorMessage: String,
    private val timeoutErrorMessage: String,
    private val waitingMessage: String,
    private val successMessage: String
) {
    private val handler = Handler(Looper.getMainLooper())
    private var browser: Browser? = null
    private var finished = true
    private var timeoutRunnable: Runnable? = null
    private var revealRunnable: Runnable? = null
    private var syncing = false
    private var showResultToast = false
    private var updatingToast: Toast? = null

    fun sync(
        showDialog: Boolean,
        onSuccess: (Boolean) -> Unit
    ) {
        val hasCredentials = hasWebCredentials()

        if (syncing) {
            if (showDialog) {
                showResultToast = true
                if (hasCredentials) {
                    setBrowserVisible(true)
                    showWaitingUi()
                    showWebViewAfter(SYNC_WEB_VIEW_REVEAL_DELAY_MS, ::hideWaitingUi)
                } else {
                    setVisible(true)
                }
            }
            return
        }

        syncing = true
        showResultToast = showDialog || !hasCredentials

        if (showDialog && hasCredentials) {
            showWaitingUi()
        }

        start(
            webViewVisible = !hasCredentials,
            browserVisible = showDialog || !hasCredentials,
            revealWebViewDelayMs = if (showDialog && hasCredentials) SYNC_WEB_VIEW_REVEAL_DELAY_MS else null,
            onWebViewShown = ::hideWaitingUi,
            onSuccess = { hasChanged ->
                onSuccess(hasChanged)
                finishSync(
                    toastMessage = if (showResultToast || hasChanged) successMessage else null,
                    longToast = false
                )
            },
            onError = { message ->
                finishSync(toastMessage = message, longToast = true)
            },
            onCancel = {
                finishSync()
            }
        )
    }

    private fun start(
        webViewVisible: Boolean,
        browserVisible: Boolean = webViewVisible,
        revealWebViewDelayMs: Long? = null,
        onWebViewShown: () -> Unit = {},
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        finished = false
        setWebViewVisible(webViewVisible)

        timeoutRunnable = Runnable {
            finish {
                onError(timeoutErrorMessage)
            }
        }
        if (!webViewVisible && !browserVisible) {
            handler.postDelayed(timeoutRunnable!!, HIDDEN_SYNC_TIMEOUT_MS)
        }
        if (revealWebViewDelayMs != null) {
            showWebViewAfter(revealWebViewDelayMs, onWebViewShown)
        }

        try {
            browser = Browser(
                activity,
                webView,
                progressBar,
                name,
                visible = browserVisible,
                onImportSuccess = { hasChanged ->
                    finish {
                        onSuccess(hasChanged)
                    }
                },
                onImportFail = { message ->
                    finish {
                        onError(message)
                    }
                },
                onLoginCancel = {
                    finish(onCancel)
                }
            )
        } catch (e: Exception) {
            finish {
                onError(e.message ?: defaultErrorMessage)
            }
        }
    }

    private fun showWebViewAfter(delayMs: Long, onWebViewShown: () -> Unit = {}) {
        if (finished || webView.isVisible) return
        revealRunnable?.let { handler.removeCallbacks(it) }
        revealRunnable = Runnable {
            if (!finished) {
                setWebViewVisible(true)
                onWebViewShown()
            }
        }
        handler.postDelayed(revealRunnable!!, delayMs)
    }

    private fun setVisible(visible: Boolean) {
        browser?.visible = visible
        setWebViewVisible(visible)
    }

    private fun setBrowserVisible(visible: Boolean) {
        browser?.visible = visible
        if (!visible) {
            progressBar?.visibility = View.INVISIBLE
        }
    }

    private fun setWebViewVisible(visible: Boolean) {
        webView.visibility = if (visible) View.VISIBLE else View.GONE
        progressBar?.let {
            it.visibility = if (visible && it.progress in 1..99) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun finish(block: () -> Unit) {
        if (finished) return
        finished = true
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        revealRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
        revealRunnable = null
        browser?.visible = false
        browser = null
        setWebViewVisible(false)
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
        } catch (_: Exception) {
        }
        block()
    }

    private fun showWaitingUi() {
        updatingToast?.cancel()
        updatingToast = Toast.makeText(activity, waitingMessage, Toast.LENGTH_LONG).also {
            it.show()
        }
    }

    private fun hideWaitingUi() {
        updatingToast?.cancel()
        updatingToast = null
        progressBar?.visibility = View.INVISIBLE
    }

    private fun finishSync(toastMessage: String? = null, longToast: Boolean = false) {
        showResultToast = false
        syncing = false
        hideWaitingUi()
        if (toastMessage != null) {
            Toast.makeText(activity, toastMessage, if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasWebCredentials(): Boolean {
        val settingWeb = activity.getSharedPreferences("WebActivity", MODE_PRIVATE)
        val username = settingWeb.getString("Username", "")?.trim().orEmpty()
        val password = settingWeb.getString("Password", "")?.trim().orEmpty()
        return username.isNotEmpty() && password.isNotEmpty()
    }

    companion object {
        private const val HIDDEN_SYNC_TIMEOUT_MS = 60_000L
        private const val SYNC_WEB_VIEW_REVEAL_DELAY_MS = 3_000L

        fun courseTable(activity: Activity, webView: WebView, progressBar: ProgressBar?) =
            StudySystemEmbeddedSync(
                activity,
                webView,
                progressBar,
                "CourseTable",
                "课程表同步失败",
                "课程表同步超时，请稍后重试",
                "正在同步课程表...",
                "课程表同步完成"
            )

        fun examArrange(activity: Activity, webView: WebView, progressBar: ProgressBar?) =
            StudySystemEmbeddedSync(
                activity,
                webView,
                progressBar,
                "GetExamArrange",
                "考试信息同步失败",
                "考试信息同步超时，请稍后重试",
                "正在同步考试信息...",
                "考试信息同步完成"
            )
    }
}
