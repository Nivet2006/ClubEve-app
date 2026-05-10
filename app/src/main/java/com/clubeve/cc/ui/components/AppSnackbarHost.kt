package com.clubeve.cc.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.clubeve.cc.ui.theme.ThemeState

/**
 * A themed snackbar host that always has high contrast:
 * Light mode → black background, white text
 * Dark mode  → white background, black text
 */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val isDark = ThemeState.isDark
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = if (isDark) Color(0xFFF0F0F0) else Color(0xFF1A1A1A),
            contentColor  = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF0F0F0),
            actionColor   = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF0F0F0)
        )
    }
}
