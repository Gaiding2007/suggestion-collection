package com.gaiding.suggestion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Brand = Color(0xFF4E6AF0)
val BrandDeep = Color(0xFF3B55D9)
val BrandSoft = Color(0xFFE9EDFE)
val Ink = Color(0xFF191B24)
val InkSoft = Color(0xFF5C6274)
val InkFaint = Color(0xFF8C92A4)
val AppBg = Color(0xFFF5F6FA)
val LineSoft = Color(0xFFE8EAF2)

val OkGreen = Color(0xFF12855A)
val OkGreenBg = Color(0xFFE3F6EC)
val WarnRed = Color(0xFFC22A2A)
val WarnRedBg = Color(0xFFFDECEC)
val InfoCyan = Color(0xFF0E7490)
val InfoCyanBg = Color(0xFFE2F3F8)
val DeepPurple = Color(0xFF6D28D9)
val DeepPurpleBg = Color(0xFFF1EAFE)
val GreyChip = Color(0xFF6B7280)
val GreyChipBg = Color(0xFFEEF1F7)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandSoft,
    onPrimaryContainer = BrandDeep,
    secondary = Color(0xFF64748B),
    background = AppBg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0F2F8),
    onSurfaceVariant = InkSoft,
    outline = LineSoft,
    error = WarnRed,
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun SuggestionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
