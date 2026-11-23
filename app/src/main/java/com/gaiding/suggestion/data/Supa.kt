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

    /**
     * 把接口返回的英文报错翻成人话。登录页和各个对话框都走这里，
     * 免得用户看到 "Could not find the function public.update_suggestion..." 这种没法处理的东西。
     */
    fun friendly(e: Throwable): String {
        val raw = (e as? ApiException)?.message ?: e.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            raw.isBlank() -> "网络好像不太行，请重试"
            "invalid login credentials" in lower -> "邮箱或密码不对"
            "user already registered" in lower -> "这个邮箱已经注册过了，直接点「登录」"
            "email not confirmed" in lower -> "邮箱还没验证，请先去邮箱点确认链接"
            "at least 6 characters" in lower -> "密码太短，至少 6 位"
            "invalid format" in lower -> "邮箱格式不对"
            "rate limit" in lower || "security purposes" in lower -> "操作太频繁了，等一会儿再试"
            "could not find the function" in lower || "pgrst202" in lower ->
                "数据库还没升级：请到 Supabase 的 SQL Editor 把 supabase/schema.sql 重新执行一次" +
                    "（新版本加了修改建议用的函数），然后重开这个页面"
            "could not find the table" in lower || "pgrst205" in lower ->
                "数据库还没初始化：请到 Supabase 的 SQL Editor 执行 supabase/schema.sql"
            "permission denied" in lower || "pgrst301" in lower ->
                "没有权限读写数据，检查一下数据库的安全规则"
            "violates check constraint" in lower || "23514" in lower ->
                "内容不符合要求（标题 50 字以内、内容 1000 字以内、驳回理由 400 字以内）"
            "failed to fetch" in lower || "connect" in lower || "timeout" in lower -> "连不上服务器，检查一下网络"
            else -> raw
        }
    }

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
