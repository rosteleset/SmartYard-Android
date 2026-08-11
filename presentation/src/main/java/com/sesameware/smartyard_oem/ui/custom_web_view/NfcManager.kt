package com.sesameware.smartyard_oem.ui.custom_web_view

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.TagTechnology
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NfcManager(
    private val adapter: NfcAdapter
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var readerEnabled = false
    private var tagPresent = false
    private var presenceJob: Job? = null

    fun enableReader(
        activity: Activity,
        onTag: (Tag) -> Unit
    ) {
        adapter.enableReaderMode(
            activity,
            { tag ->
                mainHandler.post {
                    if (!readerEnabled || tagPresent) {
                        return@post
                    }
                    tagPresent = true
                    vibrate(activity)
                    onTag(tag)
                    monitorRemoval(tag)
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        tagPresent = false
        readerEnabled = true
    }

    fun disableReader(activity: Activity) {
        if (!readerEnabled) {
            return
        }
        readerEnabled = false
        tagPresent = false
        presenceJob?.cancel()
        presenceJob = null
        adapter.disableReaderMode(activity)
    }

    private fun monitorRemoval(tag: Tag) {
        presenceJob?.cancel()
        presenceJob = monitorScope.launch {
            val technology: TagTechnology = NfcA.get(tag) ?: NfcB.get(tag) ?: return@launch
            try {
                technology.connect()
                while (isActive && readerEnabled && technology.isConnected) {
                    delay(PRESENCE_CHECK_INTERVAL_MS)
                }
            } catch (_: Exception) {
                // A lost connection means the tag is no longer available.
            } finally {
                runCatching { technology.close() }
            }

            withContext(Dispatchers.Main) {
                if (readerEnabled) {
                    tagPresent = false
                }
            }
        }
    }

    private fun vibrate(activity: Activity) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    FIRST_READ_VIBRATION_MS,
                    FIRST_READ_VIBRATION_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(FIRST_READ_VIBRATION_MS)
        }
    }

    private companion object {
        const val FIRST_READ_VIBRATION_MS = 120L
        const val FIRST_READ_VIBRATION_AMPLITUDE = 255
        const val PRESENCE_CHECK_INTERVAL_MS = 150L
    }
}
