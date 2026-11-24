package com.gaiding.suggestion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaiding.suggestion.data.Status
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.BrandSoft
import com.gaiding.suggestion.ui.theme.DeepPurple
import com.gaiding.suggestion.ui.theme.DeepPurpleBg
import com.gaiding.suggestion.ui.theme.GreyChip
import com.gaiding.suggestion.ui.theme.GreyChipBg
import com.gaiding.suggestion.ui.theme.InfoCyan
import com.gaiding.suggestion.ui.theme.InfoCyanBg
import com.gaiding.suggestion.ui.theme.Ink
import com.gaiding.suggestion.ui.theme.InkFaint
import com.gaiding.suggestion.ui.theme.InkSoft
import com.gaiding.suggestion.ui.theme.OkGreen
import com.gaiding.suggestion.ui.theme.OkGreenBg
import com.gaiding.suggestion.ui.theme.WarnRed
import com.gaiding.suggestion.ui.theme.WarnRedBg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

fun formatTime(millis: Long): String {
    if (millis <= 0L) return ""
    return timeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

@Composable
fun StatusChip(status: Status, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        Status.Pending -> GreyChip to GreyChipBg
        Status.Accepted -> OkGreen to OkGreenBg
        Status.Rejected -> WarnRed to WarnRedBg
        Status.Resolved -> InfoCyan to InfoCyanBg
        Status.Pushed -> DeepPurple to DeepPurpleBg
    }
    Surface(color = bg, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** 「我的反馈」标识：跟在状态胶囊后面，一眼看出哪几条是自己提的。 */
@Composable
fun MineChip(modifier: Modifier = Modifier) {
    Surface(color = BrandSoft, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = "我的反馈",
            color = Brand,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun Avatar(text: String, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(BrandSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = Brand,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun InfoBanner(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Surface(color = WarnRedBg, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = WarnRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun EmptyBox(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Brand, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            desc,
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand,
            contentColor = Color.White,
            disabledContainerColor = Brand.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.85f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
        modifier = modifier.height(50.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(9.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = InkSoft,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE1E4EF)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(38.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BrandButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(38.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink)
        trailing?.invoke()
    }
}
