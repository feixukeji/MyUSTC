package com.feixu.myustc

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset


object FileHelper {
    fun read(mContext: Context?, filename: String?): String { //读取文件
        return try {
            val input: FileInputStream = mContext!!.openFileInput(filename) //打开文件输入流
            val buffer = ByteArray(input.available())
            input.read(buffer) //读取文件内容
            input.close() //关闭输入流
            return buffer.toString(Charset.defaultCharset())
        } catch (_: Exception) {
            ""
        }
    }

    fun save(mContext: Context?, filename: String?, fileContent: String) { //保存文件
        return try {
            val output: FileOutputStream = mContext!!.openFileOutput(filename, Context.MODE_PRIVATE) //使用私有模式，创建出来的文件只能被本应用访问，还会覆盖原文件
            output.write(fileContent.toByteArray()) //将String字符串以字节流的形式写入到输出流中
            output.close() //关闭输出流
        } catch (_: Exception) {
        }
    }

    fun readImage(mContext: Context?, filename: String?): Bitmap? { //读取图片文件
        return try {
            val input: FileInputStream = mContext!!.openFileInput(filename) //打开文件输入流
            val image = BitmapFactory.decodeStream(input)
            input.close() //关闭输入流
            image
        } catch (_: Exception) {
            null
        }
    }

    fun saveImage(mContext: Context?, filename: String?, image: Bitmap) { //保存图片文件
        try {
            val output: FileOutputStream = mContext!!.openFileOutput(filename, Context.MODE_PRIVATE) //使用私有模式，创建出来的文件只能被本应用访问，还会覆盖原文件
            image.compress(Bitmap.CompressFormat.JPEG, 50, output)
            output.close() //关闭输出流
        } catch (_: Exception) {
        }
    }

    fun download(mContext: Context?, fileURL: String, fileName: String, mimeType: String? = null, install: Boolean = false) { //下载文件
        val appContext = mContext?.applicationContext ?: return
        val toast = Toast.makeText(appContext, "正在开始下载...", Toast.LENGTH_LONG)
        toast.show()

        try {
            val safeFileName = safeFileName(fileName)
            val request = DownloadManager.Request(fileURL.toUri()) //指定下载地址
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //设置通知的显示类型，下载进行时和完成后显示通知
            request.setAllowedOverMetered(true) //允许在计费流量下下载
            request.setAllowedOverRoaming(true) //允许漫游时下载

            val savesToPublicDownloads = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            if (savesToPublicDownloads) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName)
            } else {
                request.setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, safeFileName)
            }

            val realMimeType = resolveMimeType(safeFileName, mimeType)
            request.setMimeType(realMimeType) //设置下载文件类型，防止下载后打不开

            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(fileURL)
            if (cookieString != null) request.addRequestHeader("Cookie", cookieString) //添加Cookie信息

            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            if (install) {
                val updateDownloadId = downloadManager.enqueue(request)
                if (!UpdateInstaller.trackDownload(appContext, updateDownloadId)) {
                    downloadManager.remove(updateDownloadId)
                    throw IOException("Unable to track update download")
                }
                return
            }

            var downloadId = -1L

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (downloadId != -1L && id == downloadId) {
                        try {
                            appContext.unregisterReceiver(this)
                        } catch (_: IllegalArgumentException) {
                            // Receiver may already have been unregistered after a duplicate broadcast.
                        }
                        toast.cancel()
                        if (!isDownloadSuccessful(downloadManager, downloadId)) {
                            Toast.makeText(appContext, "下载失败", Toast.LENGTH_SHORT).show()
                        } else {
                            val message = if (savesToPublicDownloads) {
                                "下载完成，已保存至“下载”文件夹"
                            } else {
                                "下载完成，已保存至应用下载目录"
                            }
                            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED,
            ) //注册文件下载完成的广播接收器
            try {
                downloadId = downloadManager.enqueue(request) //添加一个下载任务
            } catch (exception: Exception) {
                runCatching { appContext.unregisterReceiver(onComplete) }
                throw exception
            }

        } catch (_: Exception) {
            toast.cancel()
            Toast.makeText(appContext, "下载失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadBlob(mContext: Context?, base64Data: String, fileName: String) { //下载blob文件
        val context = mContext?.applicationContext ?: return
        val toast = Toast.makeText(context, "正在开始下载...", Toast.LENGTH_LONG)
        toast.show()

        try {
            // 解码base64数据
            val fileData = Base64.decode(base64Data, Base64.DEFAULT)
            saveBlobToDownloads(context, fileData, fileName)

            toast.cancel()
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "下载完成，已保存至“下载”文件夹"
            } else {
                "下载完成，已保存至应用下载目录"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        } catch (_: Exception) {
            toast.cancel()
            Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
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

    private fun resolveMimeType(fileName: String, mimeType: String? = null): String {
        if (!mimeType.isNullOrBlank()) return mimeType
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun safeFileName(fileName: String): String {
        return fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "download" }
    }

    private fun saveBlobToDownloads(context: Context, data: ByteArray, fileName: String) {
        val safeFileName = safeFileName(fileName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("External downloads directory is unavailable")
            val target = File(downloadsDir, safeFileName)
            target.outputStream().use { output -> output.write(data) }
            return
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeFileName)
            put(MediaStore.Downloads.MIME_TYPE, resolveMimeType(safeFileName))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Unable to create download")

        try {
            resolver.openOutputStream(uri, "w")?.use { output -> output.write(data) }
                ?: throw IOException("Unable to open download")
            val updated = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            if (updated != 1) throw IOException("Unable to publish download")
        } catch (exception: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw exception
        }
    }
}
