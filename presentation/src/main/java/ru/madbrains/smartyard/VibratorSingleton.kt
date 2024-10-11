package ru.madbrains.smartyard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread


object VibratorSingleton {
    private var vibrator: Vibrator? = null
    private var isSilentMode = false
    private val lock = Any()
    var isVibrationEnabled = true // Флаг, определяющий, включена ли вибрация


    fun getVibrator(context: Context): Vibrator? {
        synchronized(lock) {
            checkRingMode(context)
            if (vibrator == null) {
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            return vibrator
        }
    }

    fun vibrateWithMode(vibrationPattern: LongArray? = null, patternRepeat: Int = 1) {
        synchronized(lock) {
            val customVibrationPattern = longArrayOf(0, 10, 200, 500, 700, 1000, 300, 200, 50, 10)
            if (isVibrationEnabled && !isSilentMode && vibrator != null && vibrator!!.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val longVibrationEffect = VibrationEffect.createWaveform(
                        vibrationPattern ?: customVibrationPattern,
                        patternRepeat
                    )
                    vibrator?.vibrate(longVibrationEffect)
                } else {
                    vibrator?.vibrate(
                        vibrationPattern ?: customVibrationPattern,
                        patternRepeat
                    )
                }
            }
        }
    }


    fun vibrationOneShot() {
        thread {
            if (isVibrationEnabled && vibrator != null && vibrator!!.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val touchVibrationEffect = VibrationEffect.createOneShot(100, -1)
                    vibrator?.vibrate(touchVibrationEffect)
                } else {
                    vibrator?.vibrate(longArrayOf(100), -1)
                }
            }
        }
    }


    fun cancel() {
        synchronized(lock) {
            vibrator?.cancel()
        }
    }

    private fun checkRingMode(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        isSilentMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> true
            else -> false
        }
    }
}