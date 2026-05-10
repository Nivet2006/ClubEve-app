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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.DarkBg
import com.clubeve.cc.ui.theme.DarkBorder
import com.clubeve.cc.ui.theme.DarkSurface
import com.clubeve.cc.ui.theme.DarkTextPrimary
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
    val isDark = ThemeState.isDark
    val scope  = rememberCoroutineScope()

    val wipeRadius  = remember { Animatable(0f) }
    var wipeColor   by remember { mutableStateOf(Color.Transparent) }
    var wipeVisible by remember { mutableStateOf(false) }
    var btnCenterPx by remember { mutableStateOf(Offset.Zero) }
    var screenDiag  by remember { mutableFloatStateOf(2000f) }

    // Label shown in center during wipe
    var labelText   by remember { mutableStateOf("") }
    var labelAlpha  by remember { mutableFloatStateOf(0f) }
    val animAlpha   by animateFloatAsState(labelAlpha, tween(200), label = "alpha")

    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {

        // ── Full-screen wipe circle ───────────────────────────────────────────
        if (wipeVisible) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = wipeColor, radius = wipeRadius.value, center = btnCenterPx)
            }
            // Centered mode label during wipe
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

                            // Expand circle — fade label in at 30%
                            launch {
                                delay((DURATION * 0.3).toLong())
                                labelAlpha = 1f
                            }

                            wipeRadius.animateTo(
                                targetValue = screenDiag,
                                animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
                            )

                            // Flip theme at peak — on main thread so recomposition triggers
                            withContext(Dispatchers.Main) {
                                ThemeState.isDark = next
                            }

                            // Collapse — fade label out at start
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
