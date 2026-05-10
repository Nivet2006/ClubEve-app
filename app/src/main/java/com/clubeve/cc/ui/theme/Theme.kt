package com.clubeve.cc.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Theme state — singleton so any composable can read/toggle it ──────────────
object ThemeState {
    var isDark by mutableStateOf(false)
    fun toggle() { isDark = !isDark }
}

val LocalIsDark = compositionLocalOf { false }

// ── Color schemes ─────────────────────────────────────────────────────────────
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

private val DarkColorScheme = darkColorScheme(
    primary            = DarkTextPrimary,
    onPrimary          = DarkBg,
    primaryContainer   = DarkSurfaceRaised,
    onPrimaryContainer = DarkTextPrimary,
    secondary          = DarkTextMuted,
    onSecondary        = DarkBg,
    tertiary           = DarkTextMuted,
    error              = Color(0xFFCF6679),
    onError            = DarkBg,
    background         = DarkBg,
    surface            = DarkSurface,
    surfaceVariant     = DarkSurfaceRaised,
    onBackground       = DarkTextPrimary,
    onSurface          = DarkTextPrimary,
    onSurfaceVariant   = DarkTextMuted,
    outline            = DarkBorder,
    outlineVariant     = DarkBorderSubtle,
    scrim              = Color(0x66000000)
)

@Composable
fun ClubEveTheme(content: @Composable () -> Unit) {
    val isDark = ThemeState.isDark
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalIsDark provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

@Composable
fun ClubEveCCTheme(content: @Composable () -> Unit) = ClubEveTheme(content)
