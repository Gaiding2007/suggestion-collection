package com.gaiding.suggestion.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * 账号相关的接口封装：注册、登录、静默续期、退出。
 *
 * 0.0.10 起账号走独立的公共账号服务（deploy/accounts/server.js，地址 Supa.ACCOUNTS），
 * 它和本应用的数据库完全分开：以后别的 App 也能用同一批账号登录。
 * 登录成功返回 access_token（12 小时）+ refresh_token（60 天），
 * 两个都是账号服务用 HMAC 自己签的，本应用的接口只负责验签。
 */
object Auth {
    /** 续期彻底失败时回调，用来把用户踢回登录页。 */
    var onExpired: (() -> Unit)? = null

    private val mutex = Mutex()

    suspend fun signIn(email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val text = Supa.raw("POST", "/auth/login", body, base = Supa.ACCOUNTS)
        persist(Supa.obj(text))
    }

    /**
     * 注册。账号服务会直接把会话返回，所以注册完就是登录状态。
     *
     * 搬过来的老账号第一次「注册」同一个邮箱时会被认领：设上新密码，历史建议还在。
     *
     * @return 恒为 true（保留旧签名，界面那边用来判断要不要提示去邮箱验证）
     */
    suspend fun signUp(email: String, password: String): Boolean {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val text = Supa.raw("POST", "/auth/register", body, base = Supa.ACCOUNTS)
        persist(Supa.obj(text))
        return true
    }

    private fun persist(obj: JSONObject) {
        val access = obj.getString("access_token")
        val refresh = obj.getString("refresh_token")
        val user = obj.optJSONObject("user")
        SessionStore.save(
            access = access,
            refresh = refresh,
            expiresInSeconds = obj.optLong("expires_in", 43_200L),
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
            // 续期接口要求用 refresh token 当 Bearer，请求体是空的
            val text = Supa.raw("POST", "/auth/refresh", JSONObject(), refresh, base = Supa.ACCOUNTS)
            val obj = Supa.obj(text)
            SessionStore.updateTokens(
                access = obj.getString("access_token"),
                refresh = obj.getString("refresh_token"),
                expiresInSeconds = obj.optLong("expires_in", 43_200L),
            )
            SessionStore.accessToken!!
        } catch (e: Supa.ApiException) {
            SessionStore.clear()
            onExpired?.invoke()
            throw Supa.ApiException(401, "登录已失效，请重新登录")
        }
    }

    /**
     * 退出当前账号。
     *
     * @param keepAccounts true = 只退出登录态，账号留在登录页的账号列表里，点一下就能直接回来；
     *                     false = 本地 token 一起删掉。
     *
     * 服务端是无状态的（token 自校验、不入库），所以这里没有「通知服务端登出」这一步。
     */
    suspend fun signOut(keepAccounts: Boolean = true) {
        if (keepAccounts) {
            SessionStore.leaveCurrent()
            return
        }
        SessionStore.clear()
    }
}
