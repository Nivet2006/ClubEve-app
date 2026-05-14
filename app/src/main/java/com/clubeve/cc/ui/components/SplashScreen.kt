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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.Mono
import kotlinx.coroutines.delay

/**
 * Full-screen splash with a morphing exit:
 *
 * Timeline:
 *   0        → text fades IN on white bg          (700 ms)
 *   700      → hold on white                      (800 ms)
 *   1 500    → bg + text cross-fade white→dark    (1 200 ms)
 *   2 700    → hold at dark                       (600 ms)
 *   3 300    → bg fades out, text shrinks 28→22sp (600 ms)  ← morphs into login title
 *   3 900    → text fades out                     (250 ms)
 *   4 150    → [onDone] called
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val isGlass = GlassState.isGlass

    // 0..1  text opacity during fade-in phase
    val textAlpha   = remember { Animatable(0f) }
    // 0..1  bg progress: 0=white, 1=dark
    val bgProgress  = remember { Animatable(0f) }
    // 0..1  bg alpha during exit (1=opaque, 0=transparent)
    val bgAlpha     = remember { Animatable(1f) }
    // 0..1  text shrink progress: 0=28sp, 1=22sp
    val shrinkProg  = remember { Animatable(0f) }
    // 0..1  final text fade-out
    val textFadeOut = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Text fades in on white
        textAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))

        // 2. Hold on white
        delay(800)

        // 3. Bg + text color cross-fade white → dark
        bgProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))

        // 4. Hold at dark
        delay(600)

        // 5. Bg fades out + text shrinks simultaneously (morphs into login title)
        bgAlpha.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        shrinkProg.animateTo(1f, tween(600, easing = FastOutSlowInEasing))

        // 6. Text fades out
        textFadeOut.animateTo(0f, tween(250, easing = FastOutSlowInEasing))

        onDone()
    }

    val p = bgProgress.value

    // Text color: black → white during phase 3, stays white during exit
    val textColor = lerp(Color.Black, Color.White, p)

    // Font size: 28sp → 22sp during exit phase
    val fontSize: TextUnit = lerp(28.sp, 22.sp, shrinkProg.value)

    // Letter spacing: 6sp → 2sp during exit
    val letterSpacing: TextUnit = lerp(6.sp, 2.sp, shrinkProg.value)

    // Combined text alpha
    val combinedTextAlpha = textAlpha.value * textFadeOut.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background — fades out in exit phase
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha.value)
        ) {
            if (isGlass && p > 0f) {
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(lerp(Color.White, Color.Black, p))
                )
            }
        }

        // Text — stays centered, shrinks and fades during exit
        Text(
            text = "CLUB-EVE",
            fontFamily = Mono,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            color = textColor,
            modifier = Modifier.alpha(combinedTextAlpha)
        )
    }
}
