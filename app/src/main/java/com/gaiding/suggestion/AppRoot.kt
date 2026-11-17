package com.gaiding.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gaiding.suggestion.data.Auth
import com.gaiding.suggestion.data.Repo
import com.gaiding.suggestion.data.SessionStore
import com.gaiding.suggestion.data.Supa
import com.gaiding.suggestion.ui.AdminHome
import com.gaiding.suggestion.ui.LoginScreen
import com.gaiding.suggestion.ui.UserHome
import com.gaiding.suggestion.ui.theme.AppBg
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.InkFaint
import kotlinx.coroutines.launch

data class Account(val id: String, val email: String, val isAdmin: Boolean)

class AppSession {
    var booting by mutableStateOf(true)
    var account by mutableStateOf<Account?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)

    suspend fun restore() {
        if (SessionStore.hasSession) {
            try {
                bindProfile()
            } catch (e: Exception) {
                account = null
                if (e is Supa.ApiException && e.status == 401) {
                    // 登录真的失效了，清掉本地的 token
                    SessionStore.clear()
                } else {
                    // 只是网络或数据库暂时不可用，token 留着，下次启动再试
                    error = friendly(e)
                }
            }
        }
        booting = false
    }

    private suspend fun bindProfile() {
        val p = Repo.myProfile()
        account = Account(id = p.id, email = p.email, isAdmin = p.isAdmin)
    }

    suspend fun signIn(email: String, password: String) {
        busy = true
        error = null
        notice = null
        try {
            Auth.signIn(email, password)
        } catch (e: Exception) {
            SessionStore.clear()
            account = null
            error = friendly(e)
            busy = false
            return
        }
        try {
            bindProfile()
        } catch (e: Exception) {
            // 账号密码是对的，只是资料没读出来（通常是数据库还没建好），token 留着
            error = friendly(e)
        }
        busy = false
    }

    suspend fun signUp(email: String, password: String) {
        busy = true
        error = null
        notice = null
        val loggedIn = try {
            Auth.signUp(email, password)
        } catch (e: Exception) {
            SessionStore.clear()
            account = null
            error = friendly(e)
            busy = false
            return
        }
        if (!loggedIn) {
            notice = "注册成功！请先去邮箱点确认链接，然后回来登录"
            busy = false
            return
        }
        try {
            bindProfile()
        } catch (e: Exception) {
            error = friendly(e)
        }
        busy = false
    }

    suspend fun signOut() {
        Auth.signOut()
        account = null
        error = null
        notice = null
    }

    private fun friendly(e: Throwable): String {
        val raw = (e as? Supa.ApiException)?.message ?: e.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            raw.isBlank() -> "网络好像不太行，请重试"
            "invalid login credentials" in lower -> "邮箱或密码不对"
            "user already registered" in lower -> "这个邮箱已经注册过了，直接点「登录」"
            "email not confirmed" in lower -> "邮箱还没验证，请先去邮箱点确认链接"
            "at least 6 characters" in lower -> "密码太短，至少 6 位"
            "invalid format" in lower -> "邮箱格式不对"
            "rate limit" in lower || "security purposes" in lower -> "操作太频繁了，等一会儿再试"
            "could not find the table" in lower || "pgrst205" in lower ->
                "数据库还没初始化：请到 Supabase 的 SQL Editor 执行 supabase/schema.sql"
            "permission denied" in lower || "pgrst301" in lower ->
                "没有权限读写数据，检查一下数据库的安全规则"
            "failed to fetch" in lower || "connect" in lower || "timeout" in lower -> "连不上服务器，检查一下网络"
            else -> raw
        }
    }
}

@Composable
fun AppRoot() {
    val session = remember { AppSession() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Auth.onExpired = { session.account = null }
        session.restore()
    }

    val account = session.account
    when {
        session.booting -> Splash()
        account == null -> LoginScreen(
            busy = session.busy,
            error = session.error,
            notice = session.notice,
            onSubmit = { register, email, password ->
                scope.launch {
                    if (register) session.signUp(email, password) else session.signIn(email, password)
                }
            },
        )
        account.isAdmin -> AdminHome(
            account = account,
            onSignOut = { scope.launch { session.signOut() } },
        )
        else -> UserHome(
            account = account,
            onSignOut = { scope.launch { session.signOut() } },
        )
    }
}

@Composable
private fun Splash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Brand,
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "建议收集",
                style = MaterialTheme.typography.titleMedium,
                color = InkFaint,
            )
        }
    }
}
