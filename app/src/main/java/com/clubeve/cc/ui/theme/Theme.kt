package com.clubeve.cc.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = AccentPrimary,
    onPrimary        = TextPrimary,
    primaryContainer = AccentGlow,
    secondary        = TextSecondary,
    background       = BackgroundPrimary,
    surface          = BackgroundSurface,
    surfaceVariant   = BackgroundElevated,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = StatusError,
    outline          = BorderDefault
)

@Composable
fun ClubEveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}

// Alias for backward compat
@Composable
fun ClubEveCCTheme(content: @Composable () -> Unit) = ClubEveTheme(content)
