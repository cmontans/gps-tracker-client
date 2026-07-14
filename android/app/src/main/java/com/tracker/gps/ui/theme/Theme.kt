package com.tracker.gps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Surfr palette — shared with web-v2 design tokens.
val Accent = Color(0xFF22D3EE)       // cyan
val Accent2 = Color(0xFF38BDF8)      // blue
val Bg = Color(0xFF0B1220)
val Surface = Color(0xFF121C2E)
val SurfaceVariant = Color(0xFF17223A)
val Outline = Color(0xFF243149)
val TextPrimary = Color(0xFFE8EEF8)
val TextMuted = Color(0xFF93A4C0)
val Green = Color(0xFF34D399)
val Red = Color(0xFFF87171)

// Back-compat aliases (older screens referenced these names).
val NeonCyan = Accent
val ElectricBlue = Accent2
val DarkGrey = Bg
val SurfaceGrey = Surface

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF06212A),
    secondary = Accent2,
    onSecondary = Color(0xFF04121F),
    tertiary = Green,
    onTertiary = Color(0xFF04231A),
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = Outline,
    error = Red,
    onError = Color(0xFF2A0A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E7490),
    onPrimary = Color.White,
    secondary = Accent2,
    tertiary = Color(0xFF059669),
    background = Color(0xFFEEF2F8),
    onBackground = Color(0xFF0F1B2D),
    surface = Color.White,
    onSurface = Color(0xFF0F1B2D),
    surfaceVariant = Color(0xFFE9EFF7),
    onSurfaceVariant = Color(0xFF56657E),
    outline = Color(0xFFD6DEEA),
    error = Color(0xFFDC2626)
)

@Composable
fun JumpTrackerTheme(
    darkTheme: Boolean = true, // dark-first, matching the web clients
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content
    )
}
