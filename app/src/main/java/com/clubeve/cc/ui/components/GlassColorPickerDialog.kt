package com.clubeve.cc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clubeve.cc.ui.theme.GlassBorderColor
import com.clubeve.cc.ui.theme.GlassSurface
import com.clubeve.cc.ui.theme.GlassTextMuted
import com.clubeve.cc.ui.theme.GlassTextPrimary
import com.clubeve.cc.ui.theme.Mono
import kotlin.math.*

/**
 * A glassmorphism-styled HSV color wheel dialog.
 * Shows a hue ring + saturation/value square + brightness slider.
 */
@Composable
fun GlassColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Decompose initial color to HSV
    val initHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                (initialColor.alpha * 255).toInt(),
                (initialColor.red   * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue  * 255).toInt()
            ), hsv
        )
        hsv
    }

    var hue        by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value      by remember { mutableFloatStateOf(initHsv[2]) }

    val currentColor by remember(hue, saturation, value) {
        derivedStateOf { Color.hsv(hue, saturation, value) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xCC0D0D2B))
                .border(1.dp, GlassBorderColor, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title
                Text(
                    text = "GLASS ACCENT",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    color = GlassTextMuted
                )

                // Color wheel (hue ring)
                HueWheel(
                    hue = hue,
                    onHueChange = { hue = it },
                    modifier = Modifier.size(220.dp)
                )

                // SV square
                SatValSquare(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSatValChange = { s, v -> saturation = s; value = v },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                // Brightness slider
                BrightnessSlider(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                // Preview swatch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, GlassBorderColor, RoundedCornerShape(12.dp))
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GlassTextMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor)
                    ) {
                        Text("Cancel", fontFamily = Mono, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentColor,
                            contentColor = if (value > 0.5f) Color.Black else Color.White
                        )
                    ) {
                        Text("Apply", fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Hue ring ──────────────────────────────────────────────────────────────────
@Composable
private fun HueWheel(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Canvas(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    onHueChange(angle)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    onHueChange(angle)
                }
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outerR = minOf(cx, cy)
        val ringW  = outerR * 0.18f
        val innerR = outerR - ringW

        // Draw hue ring as 360 thin arcs
        val sweepStep = 1f
        for (i in 0 until 360) {
            val color = Color.hsv(i.toFloat(), 1f, 1f)
            drawArc(
                color = color,
                startAngle = i.toFloat(),
                sweepAngle = sweepStep + 0.5f,
                useCenter = false,
                topLeft = Offset(cx - outerR, cy - outerR),
                size = androidx.compose.ui.geometry.Size(outerR * 2, outerR * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = ringW)
            )
        }

        // Selector dot on ring
        val angleRad = Math.toRadians(hue.toDouble())
        val dotR = (innerR + ringW / 2f)
        val dotX = cx + dotR * cos(angleRad).toFloat()
        val dotY = cy + dotR * sin(angleRad).toFloat()
        drawCircle(Color.White, radius = ringW * 0.45f, center = Offset(dotX, dotY))
        drawCircle(Color.hsv(hue, 1f, 1f), radius = ringW * 0.32f, center = Offset(dotX, dotY))
    }
}

// ── Saturation / Value square ─────────────────────────────────────────────────
@Composable
private fun SatValSquare(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Canvas(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(s, v)
                }
            }
    ) {
        // White → hue gradient (left to right)
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.White, Color.hsv(hue, 1f, 1f))
            )
        )
        // Transparent → black gradient (top to bottom)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black)
            )
        )

        // Selector crosshair
        val cx = saturation * this.size.width
        val cy = (1f - value) * this.size.height
        drawCircle(Color.White, radius = 10f, center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        drawCircle(Color.Black, radius = 7f, center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
    }
}

// ── Brightness slider ─────────────────────────────────────────────────────────
@Composable
private fun BrightnessSlider(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Black, Color.hsv(hue, saturation, 1f))
                )
            )
            // Thumb
            val thumbX = value * this.size.width
            val thumbY = this.size.height / 2f
            drawCircle(Color.White, radius = this.size.height * 0.45f, center = Offset(thumbX, thumbY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}


