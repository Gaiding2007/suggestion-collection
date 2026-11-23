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
import com.gaiding.suggestion.data.SavedAccount
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
    var savedAccounts by mutableStateOf<List<SavedAccount>>(emptyList())

    /** 登录页要不要直接显示邮箱密码表单（点「用其他账号登录」时为 true）。 */
    var wantForm by mutableStateOf(false)

    private fun refreshSaved() {
        savedAccounts = SessionStore.savedAccounts()
    }

    suspend fun restore() {
        refreshSaved()
        if (SessionStore.hasSession) {
            try {
                bindProfile()
            } catch (e: Exception) {
                account = null
                if (e is Supa.ApiException && e.status == 401) {
                    // 登录真的失效了，清掉本地的 token
                    SessionStore.clear()
                    refreshSaved()
                } else {
                    // 只是网络或数据库暂时不可用，token 留着，下次启动再试
                    error = friendly(e)
                }
            }
        }
        booting = false
    }

    /** 点登录页上的账号就切过去，不用重新输密码。 */
    suspend fun switchAccount(uid: String) {
        error = null
        notice = null
        wantForm = false
        if (!SessionStore.switchTo(uid)) {
            refreshSaved()
            return
        }
        busy = true
        try {
            bindProfile()
        } catch (e: Exception) {
            account = null
            error = friendly(e)
            if (e is Supa.ApiException && e.status == 401) SessionStore.clear()
        }
        refreshSaved()
        busy = false
    }

    /** 把这个账号从「记住的账号」里删掉，下次要重新输密码。 */
    fun forgetAccount(uid: String) {
        SessionStore.forget(uid)
        if (account?.id == uid) account = null
        refreshSaved()
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
        wantForm = false
        refreshSaved()
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
        wantForm = false
        refreshSaved()
        busy = false
    }

    suspend fun signOut() {
        // 只退出登录态，账号还留在登录页的列表里，点一下就能回来
        Auth.signOut(keepAccounts = true)
        account = null
        error = null
        notice = null
        wantForm = false
        refreshSaved()
    }

    /** 「用其他账号登录」：退出当前账号，并直接打开邮箱密码表单。 */
    suspend fun signOutToForm() {
        Auth.signOut(keepAccounts = true)
        account = null
        error = null
        notice = null
        wantForm = true
        refreshSaved()
    }

    private fun friendly(e: Throwable): String = Supa.friendly(e)
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
            saved = session.savedAccounts,
            startWithForm = session.wantForm,
            onFormConsumed = { session.wantForm = false },
            onSubmit = { register, email, password ->
                scope.launch {
                    if (register) session.signUp(email, password) else session.signIn(email, password)
                }
            },
            onSwitch = { uid -> scope.launch { session.switchAccount(uid) } },
            onForget = { uid -> session.forgetAccount(uid) },
        )
        account.isAdmin -> AdminHome(
            account = account,
            savedAccounts = session.savedAccounts,
            onSwitchAccount = { uid -> scope.launch { session.switchAccount(uid) } },
            onUseOtherAccount = { scope.launch { session.signOutToForm() } },
            onSignOut = { scope.launch { session.signOut() } },
        )
        else -> UserHome(
            account = account,
            savedAccounts = session.savedAccounts,
            onSwitchAccount = { uid -> scope.launch { session.switchAccount(uid) } },
            onUseOtherAccount = { scope.launch { session.signOutToForm() } },
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
