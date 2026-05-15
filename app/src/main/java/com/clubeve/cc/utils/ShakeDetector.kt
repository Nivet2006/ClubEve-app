package com.clubeve.cc.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects a lateral shake gesture using the accelerometer.
 *
 * Fires [onShake] at most once per [cooldownMs] to prevent rapid re-triggering.
 * Call [start] when the screen is visible and [stop] when it leaves composition.
 *
 * Threshold tuning:
 *   - [shakeThreshold] ~12–14 m/s² catches a deliberate side-to-side shake
 *     without false-positives from normal walking/pocket movement.
 *   - [cooldownMs] 1 500 ms prevents double-fires from a single shake motion.
 */
class ShakeDetector(
    context: Context,
    private val shakeThreshold: Float = 13f,   // m/s²
    private val cooldownMs: Long = 1_500L,
    private val onShake: () -> Unit
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Net acceleration excluding gravity (gravity ≈ 9.8 m/s²)
            val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            if (gForce > shakeThreshold) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > cooldownMs) {
                    lastShakeTime = now
                    onShake()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
