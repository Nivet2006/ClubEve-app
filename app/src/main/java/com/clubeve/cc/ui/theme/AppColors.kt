package com.clubeve.cc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware color aliases.
 * Use these everywhere instead of hardcoded Black/White/etc.
 * They automatically flip between light and dark mode.
 */
object AppColors {
    val bg: Color          @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
    val surface: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    val surfaceVar: Color  @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
    val onBg: Color        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground
    val onSurface: Color   @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val primary: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color   @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary
    val outline: Color     @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
    val outlineVar: Color  @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
    val error: Color       @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
}
