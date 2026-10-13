package com.feixu.myustc

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.IOException


object UpdateInstaller {
    private const val PREFERENCES_NAME = "UpdateInstaller"
    private const val TRACKED_DOWNLOADS_KEY = "TrackedDownloads"
    private const val PENDING_INSTALL_URI_KEY = "PendingInstallUri"
    private const val INSTALL_STATUS_ACTION = "com.feixu.myustc.UPDATE_INSTALL_STATUS"
    private const val APK_URI_EXTRA = "com.feixu.myustc.extra.APK_URI"
    private const val NOTIFICATION_CHANNEL_ID = "app_update"
    private const val NOTIFICATION_ID = 2001
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("UseKtx")
    fun trackDownload(context: Context, downloadId: Long): Boolean = synchronized(this) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val trackedDownloads = preferences.getStringSet(TRACKED_DOWNLOADS_KEY, emptySet()).orEmpty().toMutableSet()
        trackedDownloads.add(downloadId.toString())
        preferences.edit().putStringSet(TRACKED_DOWNLOADS_KEY, trackedDownloads).commit()
    }

    fun isTrackedDownload(context: Context, downloadId: Long): Boolean {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getStringSet(TRACKED_DOWNLOADS_KEY, emptySet())
            .orEmpty()
            .contains(downloadId.toString())
    }

    fun clearTrackedDownload(context: Context, downloadId: Long) = synchronized(this) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val trackedDownloads = preferences.getStringSet(TRACKED_DOWNLOADS_KEY, emptySet()).orEmpty().toMutableSet()
        trackedDownloads.remove(downloadId.toString())
        preferences.edit { putStringSet(TRACKED_DOWNLOADS_KEY, trackedDownloads) }
    }

    fun installDownloadedUpdate(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        if (!isDownloadSuccessful(downloadManager, downloadId)) {
            showToast(context, "更新包下载失败")
            return
        }

        val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
        if (apkUri == null) {
            showToast(context, "无法打开下载的更新包")
            return
        }

        showToast(context, "下载完成，正在安装更新")
        val silentInstallStarted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.packageManager.canRequestPackageInstalls() &&
            startSilentInstall(context, apkUri)
        if (!silentInstallStarted) requestUserInstall(context, apkUri)
    }

    fun handleInstallStatus(context: Context, intent: Intent) {
        val apkUri = intent.getStringExtra(APK_URI_EXTRA)?.toUri() ?: return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_SUCCESS -> {
                clearPendingInstall(context)
                cancelInstallNotification(context)
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = getConfirmationIntent(intent) ?: createInstallIntent(apkUri)
                requestUserAction(context, confirmationIntent, apkUri)
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                showToast(context, "已取消安装更新")
            }

            else -> {
                showToast(context, "静默安装失败，请确认安装")
                requestUserInstall(context, apkUri)
            }
        }
    }

    fun resumePendingInstall(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val apkUri = preferences.getString(PENDING_INSTALL_URI_KEY, null)?.toUri() ?: return
        preferences.edit { remove(PENDING_INSTALL_URI_KEY) }
        try {
            context.startActivity(createInstallIntent(apkUri))
        } catch (_: Exception) {
            preferences.edit { putString(PENDING_INSTALL_URI_KEY, apkUri.toString()) }
            showToast(context, "无法打开安装程序")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startSilentInstall(context: Context, apkUri: Uri): Boolean {
        val packageInstaller = context.packageManager.packageInstaller
        var sessionId = -1
        return try {
            val parameters = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            sessionId = packageInstaller.createSession(parameters)
            packageInstaller.openSession(sessionId).use { session ->
                val apkSize = context.contentResolver.openAssetFileDescriptor(apkUri, "r")?.use { it.length } ?: -1L
                val input = context.contentResolver.openInputStream(apkUri)
                    ?: throw IOException("Unable to open downloaded update")
                input.use {
                    session.openWrite("base.apk", 0L, apkSize).use { output ->
                        it.copyTo(output)
                        session.fsync(output)
                    }
                }

                val statusIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                    action = INSTALL_STATUS_ACTION
                    data = "myustc://update/$sessionId".toUri()
                    putExtra(APK_URI_EXTRA, apkUri.toString())
                }
                val statusPendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(statusPendingIntent.intentSender)
            }
            true
        } catch (_: Exception) {
            if (sessionId != -1) runCatching { packageInstaller.abandonSession(sessionId) }
            false
        }
    }

    private fun requestUserInstall(context: Context, apkUri: Uri) {
        requestUserAction(context, createInstallIntent(apkUri), apkUri)
    }

    private fun requestUserAction(context: Context, actionIntent: Intent, apkUri: Uri) {
        mainHandler.post {
            val activity = AppVisibility.currentActivity
            if (activity != null) {
                try {
                    activity.startActivity(Intent(actionIntent))
                    cancelInstallNotification(context)
                    return@post
                } catch (_: Exception) {
                }
            }

            if (!showInstallNotification(context, actionIntent)) {
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit { putString(PENDING_INSTALL_URI_KEY, apkUri.toString()) }
            }
        }
    }

    private fun showInstallNotification(context: Context, actionIntent: Intent): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL_ID, "应用更新", NotificationManager.IMPORTANCE_HIGH),
        )
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).importance == NotificationManager.IMPORTANCE_NONE) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        return try {
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(actionIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_update)
                .setContentTitle("更新包已下载")
                .setContentText("点击继续安装新版本")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun cancelInstallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun clearPendingInstall(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit { remove(PENDING_INSTALL_URI_KEY) }
    }

    private fun createInstallIntent(apkUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun isDownloadSuccessful(downloadManager: DownloadManager, downloadId: Long): Boolean {
        return runCatching {
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                cursor.moveToFirst() &&
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            } == true
        }.getOrDefault(false)
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    @Suppress("DEPRECATION")
    private fun getConfirmationIntent(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
    }
}
