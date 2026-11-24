package com.gaiding.suggestion.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaiding.suggestion.Account
import com.gaiding.suggestion.data.AdminBundle
import com.gaiding.suggestion.data.AdminSuggestion
import com.gaiding.suggestion.data.Profile
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
import com.gaiding.suggestion.ui.theme.InfoCyan
import com.gaiding.suggestion.ui.theme.Ink
import com.gaiding.suggestion.ui.theme.InkFaint
import com.gaiding.suggestion.ui.theme.InkSoft
import com.gaiding.suggestion.ui.theme.LineSoft
import com.gaiding.suggestion.ui.theme.OkGreen
import com.gaiding.suggestion.ui.theme.WarnRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHome(
    account: Account,
    savedAccounts: List<SavedAccount> = emptyList(),
    onSwitchAccount: (uid: String) -> Unit = {},
    onUseOtherAccount: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var bundle by remember { mutableStateOf<AdminBundle?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var askLogout by remember { mutableStateOf(false) }
    var askSwitch by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableIntStateOf(0) }

    suspend fun reload() {
        try {
            bundle = Repo.adminBundle()
            error = null
        } catch (e: Exception) {
            error = Supa.friendly(e)
        }
    }

    fun action(okMessage: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                reload()
                snackbar.showSnackbar(okMessage)
            } catch (e: Exception) {
                snackbar.showSnackbar(Supa.friendly(e))
            }
        }
    }

    LaunchedEffect(reloadTick) {
        if (bundle == null) loading = true
        reload()
        loading = false
    }

    val data = bundle
    val rows = data?.toRows()?.sortedBy { it.suggestion.createdAt }.orEmpty()
    // 新反馈 = 还没表态的；待办 = 已经接纳、还没推送的；已解决 = 推送完的 + 驳回掉的
    val fresh = rows.filter { it.suggestion.status == Status.Pending }
    val doing = rows.filter {
        it.suggestion.status == Status.Accepted || it.suggestion.status == Status.Resolved
    }
    val done = rows
        .filter { it.suggestion.status == Status.Pushed || it.suggestion.status == Status.Rejected }
        .sortedByDescending { it.suggestion.pushedAt ?: it.suggestion.rejectedAt ?: 0L }

    Scaffold(
        containerColor = AppBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("管理台", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Text(
                            text = account.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                actions = {
                    TextButton(onClick = { askSwitch = true }) {
                        Text("切换", color = InkSoft)
                    }
                    IconButton(onClick = { reloadTick++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = InkSoft)
                    }
                    IconButton(onClick = { askLogout = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录", tint = InkSoft)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AdminTabs(
                tab = tab,
                labels = listOf(
                    "新反馈 ${fresh.size}",
                    "待办 ${doing.size}",
                    "已解决 ${done.size}",
                    "管理",
                ),
                onSelect = { tab = it },
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                when {
                    loading -> LoadingBlock()
                    data == null -> Column(Modifier.padding(18.dp)) {
                        InfoBanner(error)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("重新加载", Modifier.fillMaxWidth()) { reloadTick++ }
                    }
                    tab == 0 -> AdminList(
                        rows = fresh,
                        numbered = true,
                        emptyTitle = "还没有新反馈",
                        emptyDesc = "有人提建议就会出现在这里，按提问顺序从早到晚排",
                        emptyIcon = Icons.Default.Star,
                        onAccept = { s -> action("已接纳") { Repo.accept(s.id) } },
                        onReject = { s, reason -> action("已驳回") { Repo.reject(s.id, reason) } },
                        onResolve = { s -> action("已标记为解决") { Repo.resolve(s.id) } },
                        onPush = { s, version, note ->
                            action("已推送，建议移入已解决") { Repo.push(s.id, version, note) }
                        },
                        onReopen = { s -> action("已退回待办") { Repo.reopen(s.id) } },
                    )
                    tab == 1 -> AdminList(
                        rows = doing,
                        numbered = false,
                        emptyTitle = "待办是空的",
                        emptyDesc = "接纳过的建议会进这里，解决后推送就完成了",
                        emptyIcon = Icons.Default.Check,
                        onAccept = { s -> action("已接纳") { Repo.accept(s.id) } },
                        onReject = { s, reason -> action("已改为驳回") { Repo.reject(s.id, reason) } },
                        onResolve = { s -> action("已标记为解决") { Repo.resolve(s.id) } },
                        onPush = { s, version, note ->
                            action("已推送，建议移入已解决") { Repo.push(s.id, version, note) }
                        },
                        onReopen = { s -> action("已退回待办") { Repo.reopen(s.id) } },
                    )
                    tab == 2 -> AdminList(
                        rows = done,
                        numbered = false,
                        emptyTitle = "还没有已解决的反馈",
                        emptyDesc = "推送过的、以及被驳回的建议都会留在这里",
                        emptyIcon = Icons.Default.Done,
                        onAccept = { s -> action("已接纳") { Repo.accept(s.id) } },
                        onReject = { s, reason -> action("已改为驳回") { Repo.reject(s.id, reason) } },
                        onResolve = { s -> action("已标记为解决") { Repo.resolve(s.id) } },
                        onPush = { s, version, note ->
                            action("已推送") { Repo.push(s.id, version, note) }
                        },
                        onReopen = { s -> action("已退回待办") { Repo.reopen(s.id) } },
                    )
                    else -> ManageTab(
                        bundle = data,
                        notify = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                        onCreateProject = { name, desc ->
                            Repo.createProject(name, desc)
                            reload()
                        },
                        onToggleAccess = { projectId, userId, granted ->
                            if (granted) Repo.grantAccess(projectId, userId) else Repo.revokeAccess(projectId, userId)
                            reload()
                        },
                        onDeleteProject = { projectId ->
                            Repo.deleteProject(projectId)
                            // 再查一遍确认真的没了：删不掉的时候不能让界面说「已删除」
                            if (Repo.projects().any { it.id == projectId }) {
                                throw Supa.ApiException(
                                    409,
                                    "项目还在：数据库没有真正删除。请确认已经在 SQL Editor 里" +
                                        "重新执行过 supabase/schema.sql，并且当前账号是管理员。",
                                )
                            }
                            reload()
                        },
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
private fun AdminList(
    rows: List<AdminSuggestion>,
    numbered: Boolean,
    emptyTitle: String,
    emptyDesc: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAccept: (Suggestion) -> Unit,
    onReject: (Suggestion, String) -> Unit,
    onResolve: (Suggestion) -> Unit,
    onPush: (Suggestion, String, String?) -> Unit,
    onReopen: (Suggestion) -> Unit,
) {
    var rejectTarget by remember { mutableStateOf<AdminSuggestion?>(null) }
    var pushTarget by remember { mutableStateOf<AdminSuggestion?>(null) }

    if (rows.isEmpty()) {
        EmptyBox(title = emptyTitle, desc = emptyDesc, icon = emptyIcon)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(rows, key = { _, row -> row.suggestion.id }) { index, row ->
            AdminSuggestionCard(
                row = row,
                index = if (numbered) index + 1 else null,
                onAccept = { onAccept(row.suggestion) },
                onReject = { rejectTarget = row },
                onResolve = { onResolve(row.suggestion) },
                onPush = { pushTarget = row },
                onReopen = { onReopen(row.suggestion) },
            )
        }
    }

    val rejectRow = rejectTarget
    if (rejectRow != null) {
        RejectDialog(
            row = rejectRow,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                rejectTarget = null
                onReject(rejectRow.suggestion, reason)
            },
        )
    }

    val pushRow = pushTarget
    if (pushRow != null) {
        PushDialog(
            row = pushRow,
            onDismiss = { pushTarget = null },
            onConfirm = { version, note ->
                pushTarget = null
                onPush(pushRow.suggestion, version, note)
            },
        )
    }
}

/** 四个页签：用等宽分段控件，字数再多也不会被截成「…」 */
@Composable
private fun AdminTabs(tab: Int, labels: List<String>, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFEDEFF7))
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = tab == index
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = if (selected) Brand else InkSoft,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun AdminSuggestionCard(
    row: AdminSuggestion,
    index: Int?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onResolve: () -> Unit,
    onPush: () -> Unit,
    onReopen: () -> Unit,
) {
    val s = row.suggestion
    var adminViewing by remember { mutableStateOf<String?>(null) }
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
                if (index != null) {
                    Text(
                        text = "#$index",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkFaint,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                ProjectChip(row.projectName)
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTime(s.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.Top) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(s.status)
            }
            val content = s.content
            if (!content.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "来自 ${row.authorLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
            )

            if (s.images.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ImageStrip(s.images) { adminViewing = it }
            }

            if (s.status == Status.Rejected && !s.rejectReason.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface_ReasonBox("驳回理由（已通知提交人）", s.rejectReason!!)
            }
            if (s.status == Status.Pushed && !s.version.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = "已在 ${s.version} 推送",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepPurple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (s.status) {
                    Status.Pending -> {
                        SecondaryButton("驳回", onClick = onReject)
                        BrandButton("接纳", onClick = onAccept)
                    }
                    Status.Accepted -> {
                        SecondaryButton("改为驳回", onClick = onReject)
                        BrandButton("标记为已解决", onClick = onResolve)
                    }
                    Status.Resolved -> {
                        SecondaryButton("改为驳回", onClick = onReject)
                        BrandButton("推送新版本", onClick = onPush)
                    }
                    Status.Rejected -> SecondaryButton("退回待办", onClick = onReopen)
                    Status.Pushed -> SecondaryButton("退回待办", onClick = onReopen)
                }
            }
        }
    }

    val viewing = adminViewing
    if (viewing != null) {
        ImageViewer(viewing) { adminViewing = null }
    }
}


@Composable
private fun ManageTab(
    bundle: AdminBundle,
    notify: (String) -> Unit,
    onCreateProject: suspend (name: String, desc: String) -> Unit,
    onToggleAccess: suspend (projectId: String, userId: String, granted: Boolean) -> Unit,
    onDeleteProject: suspend (projectId: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var accessUser by remember { mutableStateOf<Profile?>(null) }
    var deleteTarget by remember { mutableStateOf<Project?>(null) }

    val members = bundle.users.filter { !it.isAdmin }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        SectionTitle("项目") {
            BrandButton("新建项目", onClick = { showCreate = true })
        }
        Spacer(Modifier.height(10.dp))

        if (bundle.projects.isEmpty()) {
            Text(
                text = "还没有项目，点「新建项目」建第一个",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            bundle.projects.forEach { project ->
                val count = bundle.suggestions.count { it.projectId == project.id }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, LineSoft, RoundedCornerShape(16.dp)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$count 条建议",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                            )
                        }
                        val desc = project.description
                        if (!desc.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "删除项目",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarnRed,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { deleteTarget = project }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("成员权限")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "点一个成员，勾选他可以进入的项目；不勾就看不到也提不了",
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
        )
        Spacer(Modifier.height(10.dp))

        if (members.isEmpty()) {
            Text(
                text = "还没有其他人注册",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            members.forEach { member ->
                val granted = bundle.projects.count { bundle.hasAccess(it.id, member.id) }
                Card(
                    onClick = { accessUser = member },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, LineSoft, RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(member.label, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = member.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                            )
                            Text(
                                text = member.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                            )
                        }
                        Text(
                            text = "$granted 个项目",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (granted > 0) OkGreen else InkFaint,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showCreate) {
        CreateProjectDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, desc ->
                scope.launch {
                    try {
                        onCreateProject(name, desc)
                        showCreate = false
                        notify("项目已创建")
                    } catch (e: Exception) {
                        notify(Supa.friendly(e))
                    }
                }
            },
        )
    }

    val target = accessUser
    if (target != null) {
        AccessDialog(
            user = target,
            bundle = bundle,
            onDismiss = { accessUser = null },
            onToggle = { projectId, granted ->
                scope.launch {
                    try {
                        onToggleAccess(projectId, target.id, granted)
                    } catch (e: Exception) {
                        notify(Supa.friendly(e))
                    }
                }
            },
        )
    }

    val toDelete = deleteTarget
    if (toDelete != null) {
        DeleteProjectDialog(
            project = toDelete,
            count = bundle.suggestions.count { it.projectId == toDelete.id },
            onDismiss = { deleteTarget = null },
            onDelete = {
                scope.launch {
                    try {
                        onDeleteProject(toDelete.id)
                        deleteTarget = null
                        notify("项目「${toDelete.name}」已删除")
                    } catch (e: Exception) {
                        notify(Supa.friendly(e))
                    }
                }
            },
        )
    }
}

/** 彻底删项目：必须手打项目名才允许按「永久删除」。 */
@Composable
private fun DeleteProjectDialog(
    project: Project,
    count: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val confirmed = typed.trim() == project.name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除项目「${project.name}」？") },
        text = {
            Column {
                Text(
                    text = if (count > 0) {
                        "这个项目里有 $count 条建议，删除后这些建议、以及「谁能进这个项目」的设置会一起消失，没办法恢复。"
                    } else {
                        "删除后这个项目、以及「谁能进这个项目」的设置会一起消失，没办法恢复。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "想清楚就输入项目名确认：",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(30) },
                    placeholder = { Text(project.name) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, enabled = confirmed) {
                Text("永久删除", color = if (confirmed) WarnRed else InkFaint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建项目") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text("项目名称（必填）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("${name.length}/30") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { if (it.length <= 100) desc = it },
                    label = { Text("一句话说明（选填）") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), desc.trim()) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("创建", color = Brand) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AccessDialog(
    user: Profile,
    bundle: AdminBundle,
    onDismiss: () -> Unit,
    onToggle: (projectId: String, granted: Boolean) -> Unit,
) {
    var overrides by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(user.label)
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        },
        text = {
            if (bundle.projects.isEmpty()) {
                Text("还没有项目，先去新建一个项目")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    bundle.projects.forEach { project ->
                        val key = project.id
                        val checked = overrides[key] ?: bundle.hasAccess(project.id, user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val next = !checked
                                    overrides = overrides + (key to next)
                                    onToggle(project.id, next)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    val next = !checked
                                    overrides = overrides + (key to next)
                                    onToggle(project.id, next)
                                },
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun ProjectChip(name: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 150.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(BrandSoft)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Brand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RejectDialog(
    row: AdminSuggestion,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("驳回这条建议") },
        text = {
            Column {
                Text(
                    text = row.suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 400) reason = it },
                    label = { Text("驳回理由（必填）") },
                    placeholder = { Text("写清楚一点，提交人能看到") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 4,
                    supportingText = { Text("${reason.length}/400") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.trim().isNotEmpty(),
            ) { Text("确认驳回", color = WarnRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PushDialog(
    row: AdminSuggestion,
    onDismiss: () -> Unit,
    onConfirm: (version: String, note: String?) -> Unit,
) {
    var version by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("推送这条建议") },
        text = {
            Column {
                Text(
                    text = row.suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = version,
                    onValueChange = { if (it.length <= 30) version = it },
                    label = { Text("推送版本号（必填）") },
                    placeholder = { Text("例如 0.0.2") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("推送说明（选填）") },
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("${note.length}/200") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "推送后这条建议算完成，会移到「已解决」",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(version.trim(), note.trim().ifBlank { null }) },
                enabled = version.trim().isNotEmpty(),
            ) { Text("确认推送", color = Brand) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
