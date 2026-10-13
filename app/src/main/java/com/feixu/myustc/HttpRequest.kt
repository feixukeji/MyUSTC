package com.feixu.myustc

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

object HttpRequest {
    private const val TAG = "HttpRequest"
    private const val CONNECT_TIMEOUT_MILLIS = 10_000
    private const val READ_TIMEOUT_MILLIS = 15_000

    /**
     * 向指定 URL 发送 GET 请求。请求失败或服务端返回非 2xx 状态码时返回空字符串。
     */
    fun sendGet(url: String): String {
        var connection: HttpURLConnection? = null
        return try {
            connection = URI.create(url).toURL().openConnection() as? HttpURLConnection
                ?: throw IOException("Unsupported URL protocol")
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "text/plain, */*")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.errorStream?.use { }
                Log.w(TAG, "GET failed with HTTP $responseCode")
                ""
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.lineSequence().joinToString(separator = "")
                }
            }
        } catch (exception: IOException) {
            Log.w(TAG, "GET failed", exception)
            ""
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Invalid GET URL", exception)
            ""
        } finally {
            connection?.disconnect()
        }
    }
}
