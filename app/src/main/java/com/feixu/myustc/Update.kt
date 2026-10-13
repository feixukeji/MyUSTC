package com.feixu.myustc

import android.app.Activity
import android.widget.Toast

class Update(private val mActivity: Activity, private val isShowDialog: Boolean) {
    //检查更新
    private val toast = Toast.makeText(mActivity, "正在检查更新...", Toast.LENGTH_LONG)

    init {
        if (isShowDialog) toast.show()
        Thread { //网络请求要在新Thread中执行
            val latestVersionCode = HttpRequest.sendGet(mActivity.getString(R.string.myustc_url) + "version/VersionCode").toLongOrNull()
            val latestVersionFeature = HttpRequest.sendGet(mActivity.getString(R.string.myustc_url) + "version/VersionFeature").replace('|', '\n')
            mActivity.runOnUiThread {
                when { //最新版本号
                    latestVersionCode == null -> showDialog("检查更新失败，请检查网络设置")
                    BuildConfig.VERSION_CODE >= latestVersionCode -> showDialog("当前已是最新版本")
                    else -> newVersion(latestVersionFeature)
                }
            }
        }.start()
    }

    private fun showDialog(massage: String) {
        //显示提示信息
        if (isShowDialog) {
            toast.cancel()
            ThemeHelper.createDialogBuilder(mActivity).setMessage(massage).setPositiveButton("确定") { _, _ -> }.show()
        }
    }

    private fun newVersion(latestVersionFeature: String) {
        //提示有新版本
        if (isShowDialog) toast.cancel()
        ThemeHelper.createDialogBuilder(mActivity)
            .setTitle("APP有新版本，是否更新？")
            .setMessage("新版特性：$latestVersionFeature")
            .setPositiveButton("立即更新") { _, _ ->
                FileHelper.download(mActivity, mActivity.getString(R.string.myustc_url) + "MyUSTC.apk", "MyUSTC.apk", install = true)
            }
            .setNegativeButton("下次再说") { _, _ -> }
            .setCancelable(false).show()
    }
}