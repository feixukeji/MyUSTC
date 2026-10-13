package com.feixu.myustc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UpdateInstaller.handleInstallStatus(context.applicationContext, intent)
    }
}
