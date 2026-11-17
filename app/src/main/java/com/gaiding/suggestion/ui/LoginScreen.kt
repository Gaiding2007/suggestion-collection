package com.gaiding.suggestion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gaiding.suggestion.BuildConfig
import com.gaiding.suggestion.R
import com.gaiding.suggestion.data.SavedAccount
import com.gaiding.suggestion.ui.theme.AppBg
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.Ink
import com.gaiding.suggestion.ui.theme.InkFaint
import com.gaiding.suggestion.ui.theme.InkSoft
import com.gaiding.suggestion.ui.theme.LineSoft

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    notice: String?,
    saved: List<SavedAccount>,
    onSubmit: (register: Boolean, email: String, password: String) -> Unit,
    onSwitch: (uid: String) -> Unit,
    onForget: (uid: String) -> Unit,
    startWithForm: Boolean = false,
    onFormConsumed: () -> Unit = {},
) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var manualLogin by remember { mutableStateOf(startWithForm) }

    LaunchedEffect(startWithForm) {
        if (startWithForm) {
            manualLogin = true
            onFormConsumed()
        }
    }

    val showPicker = saved.isNotEmpty() && !manualLogin
    val canSubmit = email.contains("@") && password.length >= 6 && !busy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brand),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "建议收集",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "把想法写下来，会被逐条看到",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
            )

            Spacer(Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (showPicker) {
                        AccountPicker(
                            saved = saved,
                            busy = busy,
                            error = error,
                            notice = notice,
                            onSwitch = onSwitch,
                            onForget = onForget,
                            onManual = { manualLogin = true },
                        )
                    } else {
                        LoginForm(
                            register = register,
                            email = email,
                            password = password,
                            showPassword = showPassword,
                            busy = busy,
                            localError = localError,
                            error = error,
                            notice = notice,
                            canSubmit = canSubmit,
                            canGoBack = saved.isNotEmpty(),
                            onRegister = {
                                register = it
                                localError = null
                            },
                            onEmail = { email = it.trim() },
                            onPassword = { password = it },
                            onTogglePassword = { showPassword = !showPassword },
                            onBack = { manualLogin = false },
                            onSubmitClick = {
                                localError = null
                                if (!email.contains("@")) {
                                    localError = "请输入正确的邮箱"
                                } else if (password.length < 6) {
                                    localError = "密码至少 6 位"
                                } else {
                                    onSubmit(register, email, password)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 登录过的账号列表：点一下直接进，不用重新输密码。 */
@Composable
private fun AccountPicker(
    saved: List<SavedAccount>,
    busy: Boolean,
    error: String?,
    notice: String?,
    onSwitch: (uid: String) -> Unit,
    onForget: (uid: String) -> Unit,
    onManual: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "选择账号进入",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (busy) {
            Text(
                text = "正在进入…",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    InfoBanner(error)
    if (error != null) Spacer(Modifier.height(10.dp))
    if (notice != null) {
        NoticeBox(notice)
        Spacer(Modifier.height(10.dp))
    }

    saved.forEachIndexed { index, acc ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !busy) { onSwitch(acc.uid) }
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(acc.email.take(1).uppercase(), size = 36)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = acc.email.substringBefore("@").ifBlank { "这个账号" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = acc.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "移除",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !busy) { onForget(acc.uid) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        if (index != saved.lastIndex) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LineSoft),
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    SecondaryButton(
        text = "用其他账号登录",
        modifier = Modifier.fillMaxWidth(),
        onClick = onManual,
    )
}

@Composable
private fun LoginForm(
    register: Boolean,
    email: String,
    password: String,
    showPassword: Boolean,
    busy: Boolean,
    localError: String?,
    error: String?,
    notice: String?,
    canSubmit: Boolean,
    canGoBack: Boolean,
    onRegister: (Boolean) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onBack: () -> Unit,
    onSubmitClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFF1F2F8))
            .padding(4.dp),
    ) {
        SegmentTab("登录", selected = !register, Modifier.weight(1f)) { onRegister(false) }
        SegmentTab("注册", selected = register, Modifier.weight(1f)) { onRegister(true) }
    }

    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = email,
        onValueChange = onEmail,
        label = { Text("邮箱") },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text("密码（至少 6 位）") },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        trailingIcon = {
            TextButton(onClick = onTogglePassword) {
                Text(
                    text = if (showPassword) "隐藏" else "显示",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(14.dp))
    InfoBanner(localError ?: error)
    if (notice != null) {
        Spacer(Modifier.height(10.dp))
        NoticeBox(notice)
    }

    Spacer(Modifier.height(18.dp))
    PrimaryButton(
        text = if (register) "注册并进入" else "登录",
        loading = busy,
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSubmitClick,
    )

    Spacer(Modifier.height(12.dp))
    Text(
        text = if (register) {
            "注册后需要管理员给你开放项目，才能进入项目和提建议"
        } else {
            "还没有账号？点上面的「注册」"
        },
        style = MaterialTheme.typography.bodySmall,
        color = InkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (canGoBack) {
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "返回账号列表",
                style = MaterialTheme.typography.bodySmall,
                color = Brand,
            )
        }
    }
}

@Composable
private fun NoticeBox(text: String) {
    Surface(color = Color(0xFFE9F7F0), shape = RoundedCornerShape(12.dp)) {
        Text(
            text = text,
            color = Color(0xFF13795B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Ink else InkSoft,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
