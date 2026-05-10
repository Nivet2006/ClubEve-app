package com.clubeve.cc.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    // Read ThemeState.isDark directly — it's a Compose State so this composable
    // will recompose whenever the theme changes
    val isDark = ThemeState.isDark

    val bgColor  = if (isDark) Color(0xFFF5F5F5) else Color(0xFF111111)
    val txtColor = if (isDark) Color(0xFF111111) else Color(0xFFF5F5F5)

    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData  = data,
            containerColor = bgColor,
            contentColor   = txtColor,
            actionColor    = txtColor
        )
    }
}
