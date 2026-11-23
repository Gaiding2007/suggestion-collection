package com.gaiding.suggestion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gaiding.suggestion.Account
import com.gaiding.suggestion.data.Project
import com.gaiding.suggestion.data.Repo
import com.gaiding.suggestion.data.SavedAccount
import com.gaiding.suggestion.data.Status
import com.gaiding.suggestion.data.Suggestion
import com.gaiding.suggestion.data.Supa
import com.gaiding.suggestion.ui.theme.AppBg
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.BrandSoft
import com.gaiding.suggestion.ui.theme.DeepPurple
import com.gaiding.suggestion.ui.theme.Ink
import com.gaiding.suggestion.ui.theme.InkFaint
import com.gaiding.suggestion.ui.theme.InkSoft
import com.gaiding.suggestion.ui.theme.LineSoft
import com.gaiding.suggestion.ui.theme.WarnRed
import com.gaiding.suggestion.ui.theme.WarnRedBg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHome(
    account: Account,
    savedAccounts: List<SavedAccount> = emptyList(),
    onSwitchAccount: (uid: String) -> Unit = {},
    onUseOtherAccount: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    var projects by remember { mutableStateOf(emptyList<Project>()) }
    var counts by remember { mutableStateOf(emptyMap<String, Int>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<Project?>(null) }
    var askLogout by remember { mutableStateOf(false) }
    var askSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            projects = Repo.projects()
            counts = Repo.suggestionCounts()
        } catch (e: Exception) {
            error = Supa.friendly(e)
        }
        loading = false
    }

    val current = opened
    if (current != null) {
        ProjectDetail(
            project = current,
            meUid = account.id,
            onBack = { opened = null },
            onSignOut = onSignOut,
        )
        return
    }

    Scaffold(
        containerColor = AppBg,
        topBar = {
            TopAppBar(
                title = { Text("我的项目", style = MaterialTheme.typography.titleLarge, color = Ink) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                actions = {
                    TextButton(onClick = { askSwitch = true }) {
                        Text("切换", color = InkSoft)
                    }
                    IconButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = InkSoft)
                    }
                    IconButton(onClick = { askLogout = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录", tint = InkSoft)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GreetingCard(account) }

            if (loading) {
                item { LoadingBlock() }
            } else if (error != null) {
                item {
                    Column {
                        InfoBanner(error)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("重新加载", Modifier.fillMaxWidth()) { reloadKey++ }
                    }
                }
            } else if (projects.isEmpty()) {
                item {
                    EmptyBox(
                        title = "还没有可进入的项目",
                        desc = "注册成功啦，等管理员在后台给你开放项目后，这里就会出现",
                        icon = Icons.Default.Star,
                    )
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        count = counts[project.id] ?: 0,
                        onClick = { opened = project },
                    )
                }
            }
        }
    }

    if (askLogout) {
        AlertDialog(
            onDismissRequest = { askLogout = false },
            title = { Text("退出登录？") },
            text = {
                Text(
                    "退出后会回到登录页，这个账号还留在账号列表里，点一下就能直接回来。" +
                        "不想让别人进去，就在登录页点「移除」。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askLogout = false
                    onSignOut()
                }) { Text("退出", color = WarnRed) }
            },
            dismissButton = { TextButton(onClick = { askLogout = false }) { Text("取消") } },
        )
    }

    if (askSwitch) {
        AccountSwitchDialog(
            saved = savedAccounts,
            currentUid = account.id,
            onSwitch = { uid ->
                askSwitch = false
                onSwitchAccount(uid)
            },
            onUseOther = {
                askSwitch = false
                onUseOtherAccount()
            },
            onDismiss = { askSwitch = false },
        )
    }
}

@Composable
private fun GreetingCard(account: Account) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Brand, Color(0xFF7A5CF5))))
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = "你好，${account.email.substringBefore("@")}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "选一个项目进去，看看大家提了什么",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ProjectCard(project: Project, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LineSoft, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = project.name.take(1),
                    color = Brand,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                val desc = project.description
                if (!desc.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$count 条",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetail(
    project: Project,
    meUid: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    var items by remember { mutableStateOf(emptyList<Suggestion>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Suggestion?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 三块：还没被处理的、已经接纳在做的、已经推送或驳回结束的
    val fresh = items.filter { it.status == Status.Pending }
    val doing = items.filter { it.status == Status.Accepted || it.status == Status.Resolved }
    val done = items.filter { it.status == Status.Pushed || it.status == Status.Rejected }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            items = Repo.suggestionsOfProject(project.id)
        } catch (e: Exception) {
            error = Supa.friendly(e)
        }
        loading = false
    }

    BackHandler { onBack() }

    Scaffold(
        containerColor = AppBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, style = MaterialTheme.typography.titleLarge, color = Ink)
                        Text(
                            text = "新反馈 ${fresh.size} · 待办 ${doing.size} · 已解决 ${done.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                actions = {
                    IconButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = InkSoft)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("提建议") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { showNew = true },
                containerColor = Brand,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                loading -> item { LoadingBlock() }
                error != null -> item {
                    Column {
                        InfoBanner(error)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("重新加载", Modifier.fillMaxWidth()) { reloadKey++ }
                    }
                }
                items.isEmpty() -> item {
                    EmptyBox(
                        title = "这里还很安静",
                        desc = "点右下角的「提建议」，写下你的第一个想法",
                        icon = Icons.Default.Add,
                    )
                }
                else -> {
                    if (fresh.isNotEmpty()) {
                        item { GroupHeader("新反馈 ${fresh.size}") }
                        items(fresh, key = { "f-${it.id}" }) { suggestion ->
                            SuggestionCard(
                                suggestion = suggestion,
                                canEdit = suggestion.authorId == meUid,
                                onEdit = { editing = suggestion },
                            )
                        }
                    }
                    if (doing.isNotEmpty()) {
                        item { GroupHeader("待办 ${doing.size}") }
                        items(doing, key = { "d-${it.id}" }) { suggestion ->
                            SuggestionCard(suggestion)
                        }
                    }
                    if (done.isNotEmpty()) {
                        item { GroupHeader("已解决 ${done.size}") }
                        items(done, key = { "s-${it.id}" }) { suggestion ->
                            SuggestionCard(suggestion)
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        SuggestionEditorDialog(
            project = project,
            editing = null,
            onDismiss = { showNew = false },
            onDone = { message ->
                showNew = false
                reloadKey++
                scope.launch { snackbar.showSnackbar(message) }
            },
        )
    }

    val editTarget = editing
    if (editTarget != null) {
        SuggestionEditorDialog(
            project = project,
            editing = editTarget,
            onDismiss = { editing = null },
            onDone = { message ->
                editing = null
                reloadKey++
                scope.launch { snackbar.showSnackbar(message) }
            },
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = InkFaint,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
fun SuggestionCard(suggestion: Suggestion, canEdit: Boolean = false, onEdit: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LineSoft, RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(suggestion.status)
                Spacer(Modifier.weight(1f))
                if (canEdit) {
                    Text(
                        text = "修改",
                        style = MaterialTheme.typography.labelLarge,
                        color = Brand,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = formatTime(suggestion.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
            Spacer(Modifier.height(9.dp))
            SelectionContainer {
                Text(suggestion.title, style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            val content = suggestion.content
            if (!content.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (suggestion.status == Status.Rejected && !suggestion.rejectReason.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface_ReasonBox("驳回理由", suggestion.rejectReason!!)
            }
            if (suggestion.status == Status.Pushed && !suggestion.version.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                SelectionContainer {
                    Text(
                        text = "已在 ${suggestion.version} 推送",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepPurple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun Surface_ReasonBox(label: String, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarnRedBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WarnRed,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            SelectionContainer {
                Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9B2C2C))
            }
        }
    }
}

@Composable
fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Brand, strokeWidth = 2.5.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionEditorDialog(
    project: Project,
    editing: Suggestion?,
    onDismiss: () -> Unit,
    onDone: (message: String) -> Unit,
) {
    val isEdit = editing != null
    var title by remember { mutableStateOf(editing?.title.orEmpty()) }
    var content by remember { mutableStateOf(editing?.content.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            Scaffold(
                containerColor = AppBg,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (isEdit) "修改建议" else "提个建议",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Ink,
                                )
                                Text(
                                    if (isEdit) "还没被处理，可以改" else project.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkFaint,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { if (!busy) onDismiss() }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Ink)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                    )
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 50) title = it },
                        label = { Text("标题（必填）") },
                        placeholder = { Text("一句话说清楚，例如：希望支持按日期筛选") },
                        shape = RoundedCornerShape(14.dp),
                        supportingText = { Text("${title.length}/50") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 1000) content = it },
                        label = { Text("详细说明（选填）") },
                        placeholder = { Text("为什么需要、希望怎么做，写清楚更好被接纳") },
                        shape = RoundedCornerShape(14.dp),
                        minLines = 6,
                        supportingText = { Text("${content.length}/1000") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(error)
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = if (isEdit) "保存修改" else "提交建议",
                        loading = busy,
                        enabled = title.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            error = null
                            busy = true
                            scope.launch {
                                try {
                                    if (isEdit) {
                                        Repo.updateSuggestion(
                                            editing!!.id,
                                            title.trim(),
                                            content.trim().ifEmpty { null },
                                        )
                                        busy = false
                                        onDone("已保存修改")
                                    } else {
                                        Repo.createSuggestion(project.id, title, content)
                                        busy = false
                                        onDone("已提交，管理员会看到")
                                    }
                                } catch (e: Exception) {
                                    busy = false
                                    error = Supa.friendly(e)
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}
