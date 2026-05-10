package com.clubeve.cc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.Black
import com.clubeve.cc.ui.theme.DarkBg
import com.clubeve.cc.ui.theme.DarkBorder
import com.clubeve.cc.ui.theme.DarkSurface
import com.clubeve.cc.ui.theme.DarkTextPrimary
import com.clubeve.cc.ui.theme.Mono
import com.clubeve.cc.ui.theme.ThemeState
import com.clubeve.cc.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

/**
 * Floating circular theme toggle — bottom-right corner.
 *
 * On tap:
 *  1. A full-screen overlay (coloured with the INCOMING theme's background)
 *     expands as a circle from the button's exact centre — matching the
 *     web clip-path wipe effect.
 *  2. Halfway through the animation the theme flips (hidden under the overlay).
 *  3. The overlay shrinks back to nothing, revealing the new theme.
 *  4. A "Light mode" / "Dark mode" pill label fades in briefly.
 */
@Composable
fun ThemeToggleFab() {
    val isDark = ThemeState.isDark
    val scope  = rememberCoroutineScope()
    val density = LocalDensity.current

    // Radius of the wipe circle (0 → maxR → 0)
    val wipeRadius = remember { Animatable(0f) }
    // Colour of the wipe overlay (incoming theme bg)
    var wipeColor by remember { mutableStateOf(Color.Transparent) }
    // Whether the overlay is visible at all
    var wipeVisible by remember { mutableStateOf(false) }
    // Button centre in root coordinates (px)
    var btnCenterPx by remember { mutableStateOf(Offset.Zero) }
    // Screen diagonal — computed once the button is placed
    var screenDiag by remember { mutableFloatStateOf(2000f) }

    var showLabel by remember { mutableStateOf(false) }
    var busy      by remember { mutableStateOf(false) }

    // Auto-hide label
    LaunchedEffect(showLabel) {
        if (showLabel) { delay(1400); showLabel = false }
    }

    Box(Modifier.fillMaxSize()) {

        // ── Full-screen wipe overlay ──────────────────────────────────────────
        if (wipeVisible) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color  = wipeColor,
                    radius = wipeRadius.value,
                    center = btnCenterPx
                )
            }
        }

        // ── FAB + label anchored to bottom-end ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Mode label pill — appears just to the left of the button
            if (showLabel) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) DarkSurface else Black,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-62).dp, y = (-10).dp)
                ) {
                    Text(
                        text = if (isDark) "Dark mode" else "Light mode",
                        fontFamily = Mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isDark) DarkTextPrimary else White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Circular FAB
            Surface(
                shape = CircleShape,
                color = if (isDark) DarkSurface else White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(52.dp)
                    .border(
                        width = 1.dp,
                        color = if (isDark) DarkBorder else Color(0x1F000000),
                        shape = CircleShape
                    )
                    .onGloballyPositioned { coords ->
                        // Capture button centre in root px — used as wipe origin
                        val pos  = coords.positionInRoot()
                        val w    = coords.size.width.toFloat()
                        val h    = coords.size.height.toFloat()
                        btnCenterPx = Offset(pos.x + w / 2f, pos.y + h / 2f)

                        // Diagonal from button centre to farthest screen corner
                        val rootSize = coords.parentLayoutCoordinates
                            ?.size ?: return@onGloballyPositioned
                        val sw = rootSize.width.toFloat()
                        val sh = rootSize.height.toFloat()
                        screenDiag = hypot(
                            max(btnCenterPx.x, sw - btnCenterPx.x),
                            max(btnCenterPx.y, sh - btnCenterPx.y)
                        ) + 8f   // +8 px safety margin
                    }
            ) {
                IconButton(
                    onClick = {
                        if (busy) return@IconButton
                        busy = true
                        val next = !isDark

                        scope.launch {
                            val DURATION = 700
                            val HALF     = DURATION / 2

                            // Overlay colour = incoming theme background
                            wipeColor   = if (next) DarkBg else White
                            wipeVisible = true
                            wipeRadius.snapTo(0f)

                            // Expand circle to cover full screen
                            wipeRadius.animateTo(
                                targetValue = screenDiag,
                                animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
                            )

                            // Flip theme at peak (fully covered)
                            ThemeState.isDark = next
                            showLabel = true

                            // Collapse circle back to nothing
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
                        tint = if (isDark) DarkTextPrimary else Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
