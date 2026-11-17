package com.gaiding.suggestion.data

import android.content.Context
import android.content.SharedPreferences

/** 登录页上「点一下就能进」的账号。 */
data class SavedAccount(val uid: String, val email: String)

/**
 * 本地保存的登录态。
 *
 * 支持记住多个登录过的账号：每个账号单独存 access / refresh token，
 * 登录页点一下就能切过去，不用重新输密码。
 * Access token 过期后由 [Auth] 自动用 refresh token 换新的。
 */
object SessionStore {
    private const val FILE = "accounts"
    private const val KEY_CURRENT = "current"
    private const val KEY_ORDER = "order"
    private const val LEGACY_FILE = "session"

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
        migrateLegacy(context)
        loadCurrent()
    }

    /** 0.0.1 只有一个会话，升级上来时把那一个账号搬进多账号列表，不用重新登录。 */
    private fun migrateLegacy(context: Context) {
        if (prefs.contains(KEY_CURRENT)) return
        val old = context.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE)
        val uid = old.getString("uid", null)
        val refresh = old.getString("refresh", null)
        if (uid.isNullOrBlank() || refresh.isNullOrBlank()) return
        store(
            uid = uid,
            mail = old.getString("email", null).orEmpty(),
            access = old.getString("access", null).orEmpty(),
            refresh = refresh,
            expires = old.getLong("expires", 0L),
        )
        prefs.edit().putString(KEY_CURRENT, uid).apply()
        old.edit().clear().apply()
    }

    val hasSession: Boolean get() = !refreshToken.isNullOrBlank()

    private fun order(): List<String> =
        prefs.getString(KEY_ORDER, null)
            .orEmpty()
            .split(',')
            .filter { it.isNotBlank() }

    fun savedAccounts(): List<SavedAccount> = order().mapNotNull { uid ->
        prefs.getString("acc.$uid.email", null)?.let { SavedAccount(uid, it) }
    }

    private fun store(uid: String, mail: String, access: String, refresh: String, expires: Long) {
        val list = order().toMutableList()
        if (uid !in list) list.add(uid)
        prefs.edit()
            .putString(KEY_ORDER, list.joinToString(","))
            .putString("acc.$uid.email", mail)
            .putString("acc.$uid.access", access)
            .putString("acc.$uid.refresh", refresh)
            .putLong("acc.$uid.expires", expires)
            .apply()
    }

    /** 登录 / 注册成功后调用：写入并设为当前账号。 */
    fun save(access: String, refresh: String, expiresInSeconds: Long, uid: String, mail: String) {
        val expires = System.currentTimeMillis() + expiresInSeconds * 1000L
        store(uid = uid, mail = mail, access = access, refresh = refresh, expires = expires)
        prefs.edit().putString(KEY_CURRENT, uid).apply()
        loadCurrent()
    }

    /** 切到某个已记住的账号；token 失效时由 [Auth.accessToken] 自动续期。 */
    fun switchTo(uid: String): Boolean {
        if (prefs.getString("acc.$uid.refresh", null).isNullOrBlank()) return false
        prefs.edit().putString(KEY_CURRENT, uid).apply()
        loadCurrent()
        return hasSession
    }

    /** 从列表里删掉一个账号（不一定是当前账号）。 */
    fun forget(uid: String) {
        val rest = order().filter { it != uid }
        val editor = prefs.edit()
            .putString(KEY_ORDER, rest.joinToString(","))
            .remove("acc.$uid.email")
            .remove("acc.$uid.access")
            .remove("acc.$uid.refresh")
            .remove("acc.$uid.expires")
        if (prefs.getString(KEY_CURRENT, null) == uid) {
            editor.remove(KEY_CURRENT)
            clearFields()
        }
        editor.apply()
        if (userId == null) loadCurrent()
    }

    fun updateTokens(access: String, refresh: String, expiresInSeconds: Long) {
        val uid = userId ?: return
        val expires = System.currentTimeMillis() + expiresInSeconds * 1000L
        prefs.edit()
            .putString("acc.$uid.access", access)
            .putString("acc.$uid.refresh", refresh)
            .putLong("acc.$uid.expires", expires)
            .apply()
        accessToken = access
        refreshToken = refresh
        expiresAt = expires
    }

    /**
     * 退出登录，但账号留在「记住的账号」里：下次打开登录页点一下就能直接回来。
     * （token 保留，所以不用重新输密码；想彻底删掉用 [forget]）
     */
    fun leaveCurrent() {
        prefs.edit().remove(KEY_CURRENT).apply()
        clearFields()
    }

    /** 彻底忘掉当前账号：从记住的列表里一起删掉，下次必须重新输密码。 */
    fun clear() {
        val uid = userId
        if (uid == null) {
            clearFields()
            prefs.edit().clear().apply()
        } else {
            forget(uid)
        }
    }

    private fun clearFields() {
        accessToken = null
        refreshToken = null
        expiresAt = 0L
        userId = null
        email = null
    }

    private fun loadCurrent() {
        val uid = prefs.getString(KEY_CURRENT, null)
        if (uid.isNullOrBlank()) {
            clearFields()
            return
        }
        val refresh = prefs.getString("acc.$uid.refresh", null)
        if (refresh.isNullOrBlank()) {
            clearFields()
            return
        }
        userId = uid
        email = prefs.getString("acc.$uid.email", null).orEmpty()
        accessToken = prefs.getString("acc.$uid.access", null)
        refreshToken = refresh
        expiresAt = prefs.getLong("acc.$uid.expires", 0L)
    }
}
