package com.clubeve.cc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.Mono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen splash that plays once on app start.
 *
 * Timeline (total ~1 700 ms):
 *   0 ms   → text fades IN on white bg          (300 ms)
 *   700 ms → bg cross-fades white→dark, text black→white (500 ms)
 *   1 400 ms → entire splash fades OUT           (300 ms)
 *   1 700 ms → [onDone] called
 *
 * When glassmorphism is active the "dark" target is the deep purple-blue
 * radial gradient; otherwise it is solid black.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val isGlass = GlassState.isGlass

    // ── Animatables ───────────────────────────────────────────────────────────
    // textAlpha: 0 → 1 (fade-in), stays 1, then whole splash fades out via splashAlpha
    val textAlpha   = remember { Animatable(0f) }
    // bgProgress: 0 = white, 1 = dark/glass
    val bgProgress  = remember { Animatable(0f) }
    // splashAlpha: 1 → 0 (final fade-out of the whole overlay)
    val splashAlpha = remember { Animatable(1f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 1. Text fades in on white bg
        textAlpha.animateTo(
            1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )

        // 2. Hold on white
        delay(800)

        // 3. Bg + text color cross-fade simultaneously
        bgProgress.animateTo(
            1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )

        // 4. Hold at dark state
        delay(600)

        // 5. Fade out the whole splash
        splashAlpha.animateTo(
            0f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )

        onDone()
    }

    // ── Derived colors ────────────────────────────────────────────────────────
    val p = bgProgress.value   // 0..1

    // Text color: black → white
    val textColor = lerp(Color.Black, Color.White, p)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(splashAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Background layer
        if (isGlass && p > 0f) {
            // Glass: blend white → deep purple gradient
            // Draw white base first, then the gradient on top with increasing alpha
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lerp(Color.White, Color.Black, p))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(p)
                    .background(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF1A0A3D),
                                0.4f to Color(0xFF0D0D2B),
                                0.7f to Color(0xFF050518),
                                1.0f to Color(0xFF000008)
                            )
                        )
                    )
            )
        } else {
            // Normal: white → black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lerp(Color.White, Color.Black, p))
            )
        }

        // Text
        Text(
            text = "CLUB-EVE",
            fontFamily = Mono,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = 6.sp,
            color = textColor,
            modifier = Modifier.alpha(textAlpha.value)
        )
    }
}
