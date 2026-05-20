package com.tracker.gps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF2979FF)
val DarkGrey = Color(0xFF121212)
val SurfaceGrey = Color(0xFF1E1E1E)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricBlue,
    tertiary = Color(0xFF64FFDA),
    background = DarkGrey,
    surface = SurfaceGrey,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun JumpTrackerTheme(
    darkTheme: Boolean = true, // Force dark theme for visibility
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
