package com.gaiding.suggestion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 极简的 HTTP 客户端，连的是自己服务器上的接口（deploy/server/server.js）。
 *
 * 只依赖 Android 自带的 HttpURLConnection + org.json，不引入第三方网络库，
 * 这样离线环境下也能稳定构建，APK 也更小。
 *
 * 0.0.8 起后端从 Supabase 搬到阿里云：地址是 http://39.96.113.178:8080/api，
 * 走明文 http（服务器没备案、没有 https 证书），所以 AndroidManifest 里开了
 * usesCleartextTraffic。
 */
object Supa {
    const val BASE = "http://39.96.113.178:8080/api"

    class ApiException(val status: Int, override val message: String) : Exception(message)

    /**
     * 把接口返回的报错变成人话。服务端返回的就是中文（{"error":"..."}），
     * 这里主要兜住网络层和鉴权层的异常。
     */
    fun friendly(e: Throwable): String {
        val raw = (e as? ApiException)?.message ?: e.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            raw.isBlank() -> "网络好像不太行，请重试"
            "登录已失效" in raw -> "登录已失效，请重新登录"
            "邮箱或密码不对" in raw -> raw
            "at least 6 characters" in lower || "密码至少" in raw -> "密码太短，至少 6 位"
            "unable to resolve host" in lower || "failed to connect" in lower ||
                "connection refused" in lower || "timed out" in lower || "timeout" in lower ->
                "连不上服务器，检查一下网络（服务器地址 39.96.113.178:8080）"
            "cleartext" in lower -> "系统不让用明文 http，请更新到最新版本"
            else -> raw
        }
    }

    suspend fun raw(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) throw ApiException(code, friendlyError(text, code))
            text
        } finally {
            conn.disconnect()
        }
    }

    private fun friendlyError(raw: String, code: Int): String {
        if (raw.isBlank()) return "请求失败（$code）"
        return try {
            val obj = JSONObject(raw)
            val msg = when {
                obj.has("error") -> obj.optString("error")
                obj.has("msg") -> obj.optString("msg")
                obj.has("message") -> obj.optString("message")
                else -> ""
            }
            msg.takeIf { it.isNotBlank() } ?: "请求失败（$code）"
        } catch (_: Exception) {
            raw.take(180)
        }
    }

    fun obj(text: String): JSONObject = if (text.isBlank()) JSONObject() else JSONObject(text)
    fun arr(text: String): JSONArray = if (text.isBlank()) JSONArray() else JSONArray(text)
}
