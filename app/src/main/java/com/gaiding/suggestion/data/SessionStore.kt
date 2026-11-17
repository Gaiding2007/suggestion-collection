package com.gaiding.suggestion.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 本地保存的登录态。Access token 过期后由 [Supa] 自动用 refresh token 换新的。
 */
object SessionStore {
    private const val FILE = "session"
    private lateinit var prefs: SharedPreferences

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set
    var expiresAt: Long = 0L
        private set
    var userId: String? = null
        private set
    var email: String? = null
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        accessToken = prefs.getString("access", null)
        refreshToken = prefs.getString("refresh", null)
        expiresAt = prefs.getLong("expires", 0L)
        userId = prefs.getString("uid", null)
        email = prefs.getString("email", null)
    }

    val hasSession: Boolean get() = !refreshToken.isNullOrBlank()

    fun save(access: String, refresh: String, expiresInSeconds: Long, uid: String, mail: String) {
        accessToken = access
        refreshToken = refresh
        expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        userId = uid
        email = mail
        prefs.edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .putLong("expires", expiresAt)
            .putString("uid", uid)
            .putString("email", mail)
            .apply()
    }

    fun updateTokens(access: String, refresh: String, expiresInSeconds: Long) {
        accessToken = access
        refreshToken = refresh
        expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        prefs.edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .putLong("expires", expiresAt)
            .apply()
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        expiresAt = 0L
        userId = null
        email = null
        prefs.edit().clear().apply()
    }
}
