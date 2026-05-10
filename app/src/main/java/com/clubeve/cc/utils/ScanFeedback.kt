package com.clubeve.cc.utils

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

object ScanFeedback {

    /**
     * Short double-beep + single crisp vibration pulse — new check-in confirmed.
     */
    fun success(context: Context) {
        beep(ToneGenerator.TONE_PROP_BEEP2, durationMs = 120)
        vibrate(context, longArrayOf(0, 60, 40, 60), amplitudes = intArrayOf(0, 200, 0, 200))
    }

    /**
     * Low single buzz — already checked in or error.
     */
    fun warning(context: Context) {
        beep(ToneGenerator.TONE_PROP_NACK, durationMs = 200)
        vibrate(context, longArrayOf(0, 200), amplitudes = intArrayOf(0, 120))
    }

    // ─── Internals ───────────────────────────────────────────────────────────

    private fun beep(tone: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(tone, durationMs)
            // ToneGenerator releases itself after the tone completes
        } catch (_: Exception) { /* audio not available — silently skip */ }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService<VibratorManager>()
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                )
            } else {
                val v = context.getSystemService<Vibrator>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    v?.vibrate(timings, -1)
                }
            }
        } catch (_: Exception) { /* vibrator not available — silently skip */ }
    }
}
