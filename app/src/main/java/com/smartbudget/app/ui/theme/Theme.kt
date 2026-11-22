package com.smartbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SentinelColorScheme = darkColorScheme(
    primary = SentinelEmerald,
    onPrimary = Color(0xFF002817),
    primaryContainer = SentinelBgTertiary,
    onPrimaryContainer = SentinelEmerald,

    secondary = SentinelCyan,
    onSecondary = Color(0xFF002238),
    secondaryContainer = Color(0xFF16253B),
    onSecondaryContainer = SentinelCyan,

    tertiary = SentinelIndigo,
    onTertiary = Color(0xFF1A1A40),

    error = SentinelRose,
    onError = Color(0xFF3E000B),

    background = SentinelBgPrimary,
    onBackground = SentinelTextPrimary,

    surface = SentinelBgSecondary,
    onSurface = SentinelTextPrimary,

    surfaceVariant = SentinelBgTertiary,
    onSurfaceVariant = SentinelTextSecondary,

    outline = SentinelBorderSubtle,
    outlineVariant = Color(0xFF2D3748)
)

@Composable
fun SmartBudgetTheme(
    darkTheme: Boolean = true, // По умолчанию всегда держим фирменную темную кибер-тему Sentinel
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography = Typography,
        content = content
    )
}
