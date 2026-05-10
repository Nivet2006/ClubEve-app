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

// ── Glassmorphism easter-egg state ────────────────────────────────────────────
object GlassState {
    var isGlass by mutableStateOf(false)
    fun toggle() { isGlass = !isGlass }
}

val LocalIsDark = compositionLocalOf { false }
val LocalIsGlass = compositionLocalOf { false }

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

// ── Glassmorphism color scheme ────────────────────────────────────────────────
// Uses the dark scheme as base but overrides surface/background with translucent glass values.
// All screens that use MaterialTheme.colorScheme tokens automatically get the glass look.
private val GlassColorScheme = darkColorScheme(
    primary            = GlassAccent,
    onPrimary          = GlassOnAccent,
    primaryContainer   = GlassAccentContainer,
    onPrimaryContainer = GlassTextPrimary,
    secondary          = GlassTextMuted,
    onSecondary        = GlassBg,
    tertiary           = GlassTextMuted,
    error              = GlassError,
    onError            = GlassBg,
    background         = Color.Transparent,   // let the gradient in MainActivity show through
    surface            = GlassSurface,
    surfaceVariant     = GlassSurfaceRaised,
    onBackground       = GlassTextPrimary,
    onSurface          = GlassTextPrimary,
    onSurfaceVariant   = GlassTextMuted,
    outline            = GlassBorderColor,
    outlineVariant     = GlassBorderSubtle,
    scrim              = Color(0x88000000)
)

@Composable
fun ClubEveTheme(content: @Composable () -> Unit) {
    val isDark  = ThemeState.isDark
    val isGlass = GlassState.isGlass

    val colorScheme = when {
        isGlass -> GlassColorScheme
        isDark  -> DarkColorScheme
        else    -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Glass mode always uses dark status bar (light icons) since bg is dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !isDark && !isGlass
        }
    }

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalIsGlass provides isGlass
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

@Composable
fun ClubEveCCTheme(content: @Composable () -> Unit) = ClubEveTheme(content)
