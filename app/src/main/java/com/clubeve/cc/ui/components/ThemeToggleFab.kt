package com.clubeve.cc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.DarkBg
import com.clubeve.cc.ui.theme.DarkBorder
import com.clubeve.cc.ui.theme.DarkSurface
import com.clubeve.cc.ui.theme.DarkTextPrimary
import com.clubeve.cc.ui.theme.GlassBorderColor
import com.clubeve.cc.ui.theme.GlassColorStore
import com.clubeve.cc.ui.theme.GlassSurface
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.Mono
import com.clubeve.cc.ui.theme.ThemeState
import com.clubeve.cc.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun ThemeToggleFab() {
    val isDark  = ThemeState.isDark
    val isGlass = GlassState.isGlass
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    // Color picker dialog state
    var showColorPicker by remember { mutableStateOf(false) }

    val wipeRadius  = remember { Animatable(0f) }
    var wipeColor   by remember { mutableStateOf(Color.Transparent) }
    var wipeVisible by remember { mutableStateOf(false) }
    var btnCenterPx by remember { mutableStateOf(Offset.Zero) }
    var screenDiag  by remember { mutableFloatStateOf(2000f) }

    var labelText  by remember { mutableStateOf("") }
    var labelAlpha by remember { mutableFloatStateOf(0f) }
    val animAlpha  by animateFloatAsState(labelAlpha, tween(200), label = "alpha")

    var busy by remember { mutableStateOf(false) }

    // Color picker dialog
    if (showColorPicker) {
        GlassColorPickerDialog(
            initialColor = GlassState.glassAccentColor,
            onColorSelected = { newColor ->
                GlassState.glassAccentColor = newColor
                showColorPicker = false
                // Persist
                scope.launch(Dispatchers.IO) {
                    GlassColorStore.saveAccentColor(
                        context,
                        android.graphics.Color.argb(
                            (newColor.alpha * 255).toInt(),
                            (newColor.red   * 255).toInt(),
                            (newColor.green * 255).toInt(),
                            (newColor.blue  * 255).toInt()
                        ).toLong()
                    )
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Box(Modifier.fillMaxSize()) {

        // ── Full-screen wipe circle (only for dark/light toggle) ──────────────
        if (wipeVisible) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = wipeColor, radius = wipeRadius.value, center = btnCenterPx)
            }
            if (animAlpha > 0f) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = labelText,
                        fontFamily = Mono,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        color = if (isDark) DarkTextPrimary else White,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                shape = CircleShape,
                // In glass mode use a frosted glass surface; otherwise normal
                color = if (isGlass) GlassSurface else if (isDark) DarkSurface else White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(52.dp)
                    .border(
                        width = 1.dp,
                        color = if (isGlass) GlassBorderColor
                                else if (isDark) DarkBorder
                                else Color(0x1F000000),
                        shape = CircleShape
                    )
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val w   = coords.size.width.toFloat()
                        val h   = coords.size.height.toFloat()
                        btnCenterPx = Offset(pos.x + w / 2f, pos.y + h / 2f)
                        val rootSize = coords.parentLayoutCoordinates?.size ?: return@onGloballyPositioned
                        screenDiag = hypot(
                            max(btnCenterPx.x, rootSize.width  - btnCenterPx.x),
                            max(btnCenterPx.y, rootSize.height - btnCenterPx.y)
                        ) + 8f
                    }
            ) {
                if (isGlass) {
                    // In glass mode: palette icon opens color picker
                    IconButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize glass color",
                            tint = GlassState.glassAccentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Normal mode: dark/light toggle with wipe animation
                    IconButton(
                        onClick = {
                            if (busy) return@IconButton
                            busy = true
                            val next = !isDark

                            scope.launch {
                                val DURATION = 650

                                wipeColor   = if (next) DarkBg else White
                                labelText   = if (next) "Dark mode" else "Light mode"
                                wipeVisible = true
                                labelAlpha  = 0f
                                wipeRadius.snapTo(0f)

                                launch {
                                    delay((DURATION * 0.3).toLong())
                                    labelAlpha = 1f
                                }

                                wipeRadius.animateTo(
                                    targetValue = screenDiag,
                                    animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
                                )

                                withContext(Dispatchers.Main) {
                                    ThemeState.isDark = next
                                }

                                launch {
                                    delay(80)
                                    labelAlpha = 0f
                                }

                                wipeRadius.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
                                )

                                wipeVisible = false
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                            tint = if (isDark) DarkTextPrimary else Color(0xFF1A1A1A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
