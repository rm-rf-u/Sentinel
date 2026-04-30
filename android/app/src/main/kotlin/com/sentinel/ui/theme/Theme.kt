package com.sentinel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SentinelColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Surface,
    primaryContainer = Primary.copy(alpha = 0.15f),
    secondary        = Accent,
    onSecondary      = Surface,
    background       = BgWarm,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    surfaceVariant   = Border,
    onSurfaceVariant = TextSecondary,
    error            = Danger,
    onError          = Surface,
)

@Composable
fun SentinelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography  = SentinelTypography,
        content     = content,
    )
}
