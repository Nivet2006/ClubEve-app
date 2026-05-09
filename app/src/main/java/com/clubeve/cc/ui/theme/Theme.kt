package com.clubeve.cc.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light (white) scheme — minimalist B&W
private val LightColorScheme = lightColorScheme(
    primary            = Black,
    onPrimary          = White,
    primaryContainer   = LightGray,
    onPrimaryContainer = Black,
    secondary          = DarkGray,
    onSecondary        = White,
    tertiary           = MidGray,
    error              = StatusError,
    onError            = White,
    background         = White,
    surface            = OffWhite,
    surfaceVariant     = LightGray,
    onBackground       = Black,
    onSurface          = Black,
    onSurfaceVariant   = DarkGray,
    outline            = BorderDefault,
    outlineVariant     = BorderSubtle,
    scrim              = Color(0x33000000)
)

@Composable
fun ClubEveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Light status bar — dark icons on white background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}

@Composable
fun ClubEveCCTheme(content: @Composable () -> Unit) = ClubEveTheme(content)
