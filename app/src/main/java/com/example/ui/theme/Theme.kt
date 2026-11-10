package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DiskWalaDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF0A2E46),
    onPrimaryContainer = Color(0xFFB8F3FF),
    secondary = TurboIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF23295A),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = TurboGold,
    onTertiary = Color(0xFF281800),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark,
    error = CoralDanger,
    onError = Color.White
)

@Composable
fun DiskWalaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DiskWalaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
