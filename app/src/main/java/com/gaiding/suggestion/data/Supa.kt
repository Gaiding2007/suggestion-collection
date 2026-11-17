package com.gaiding.suggestion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 极简的 Supabase HTTP 客户端。
 *
 * 只依赖 Android 自带的 HttpURLConnection + org.json，不引入第三方网络库，
 * 这样离线环境下也能稳定构建，APK 也更小。
 */
object Supa {
    const val BASE = "https://pnnyujqkkdehfxnmomww.supabase.co"
    const val API_KEY = "sb_publishable__4EaDxHbbUWOXt8zvDlGaA_iLu31e2n"

    class ApiException(val status: Int, override val message: String) : Exception(message)

    suspend fun raw(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
        prefer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", API_KEY)
            setRequestProperty("Accept", "application/json")
            if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) setRequestProperty("Prefer", prefer)
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
                obj.has("msg") -> obj.optString("msg")
                obj.has("error_description") -> obj.optString("error_description")
                obj.has("message") -> obj.optString("message")
                obj.has("error") -> obj.optString("error")
                else -> ""
            }
            val hint = obj.optString("hint", "")
            val detail = obj.optString("details", "")
            listOf(msg, detail, hint).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "请求失败（$code）" }
        } catch (_: Exception) {
            raw.take(180)
        }
    }

    fun obj(text: String): JSONObject = if (text.isBlank()) JSONObject() else JSONObject(text)
    fun arr(text: String): JSONArray = if (text.isBlank()) JSONArray() else JSONArray(text)
}
