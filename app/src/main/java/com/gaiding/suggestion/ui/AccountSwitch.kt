package com.gaiding.suggestion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gaiding.suggestion.data.SavedAccount
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.BrandSoft
import com.gaiding.suggestion.ui.theme.Ink
import com.gaiding.suggestion.ui.theme.InkFaint

/**
 * 首页右上角「切换」按钮弹出来的账号列表：点一个直接进去，不用重新输密码。
 */
@Composable
fun AccountSwitchDialog(
    saved: List<SavedAccount>,
    currentUid: String?,
    onSwitch: (uid: String) -> Unit,
    onUseOther: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换账号") },
        text = {
            Column {
                Text(
                    text = "点一个账号直接进去，不用重新输密码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
                Spacer(Modifier.height(10.dp))
                saved.forEach { acc ->
                    val isCurrent = acc.uid == currentUid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSwitch(acc.uid) }
                            .background(if (isCurrent) BrandSoft else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(acc.email.take(1).uppercase(), size = 32)
                        Spacer(Modifier.width(10.dp))
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
                        if (isCurrent) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.labelSmall,
                                color = Brand,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUseOther) { Text("用其他账号登录") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
