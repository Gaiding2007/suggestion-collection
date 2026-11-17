package com.gaiding.suggestion.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Supabase Auth（GoTrue）封装：注册、登录、静默续期、退出。
 */
object Auth {
    /** 续期彻底失败时回调，用来把用户踢回登录页。 */
    var onExpired: (() -> Unit)? = null

    private val mutex = Mutex()

    suspend fun signIn(email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val text = Supa.raw("POST", "/auth/v1/token?grant_type=password", body)
        persist(Supa.obj(text))
    }

    /**
     * @return true 表示注册后已直接登录；false 表示账号已创建但需要邮箱验证。
     */
    suspend fun signUp(email: String, password: String): Boolean {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val text = Supa.raw("POST", "/auth/v1/signup", body)
        val obj = Supa.obj(text)
        return if (obj.optString("access_token").isNotBlank()) {
            persist(obj)
            true
        } else {
            false
        }
    }

    private fun persist(obj: JSONObject) {
        val access = obj.getString("access_token")
        val refresh = obj.getString("refresh_token")
        val user = obj.optJSONObject("user")
        SessionStore.save(
            access = access,
            refresh = refresh,
            expiresInSeconds = obj.optLong("expires_in", 3600L),
            uid = user?.optString("id").orEmpty(),
            mail = user?.optString("email").orEmpty(),
        )
    }

    /** 拿到一个可用的 access token，必要时自动续期。 */
    suspend fun accessToken(force: Boolean = false): String = mutex.withLock {
        val current = SessionStore.accessToken
        val stillValid = !force && current != null && SessionStore.expiresAt - System.currentTimeMillis() > 60_000
        if (stillValid) return@withLock current!!

        val refresh = SessionStore.refreshToken
        if (refresh.isNullOrBlank()) throw Supa.ApiException(401, "登录已失效，请重新登录")
        try {
            val body = JSONObject().put("refresh_token", refresh)
            val text = Supa.raw("POST", "/auth/v1/token?grant_type=refresh_token", body)
            val obj = Supa.obj(text)
            SessionStore.updateTokens(
                access = obj.getString("access_token"),
                refresh = obj.getString("refresh_token"),
                expiresInSeconds = obj.optLong("expires_in", 3600L),
            )
            SessionStore.accessToken!!
        } catch (e: Supa.ApiException) {
            SessionStore.clear()
            onExpired?.invoke()
            throw Supa.ApiException(401, "登录已失效，请重新登录")
        }
    }

    suspend fun signOut() {
        val token = SessionStore.accessToken ?: SessionStore.refreshToken
        SessionStore.clear()
        if (token != null) {
            try {
                Supa.raw("POST", "/auth/v1/logout", JSONObject(), token)
            } catch (_: Exception) {
                // 本地已经清掉了，服务端登出失败不影响使用
            }
        }
    }
}
