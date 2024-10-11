package ru.madbrains.smartyard

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.VolumeProviderCompat

class VolumeButtonService : Service() {
    private var mediaSession: MediaSessionCompat? = null
    private var isSilentMode = true
    private lateinit var audioManager: AudioManager
    private var currentVolume = 0

    override fun onCreate() {
        super.onCreate()
        audioManager =
            this.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        isSilentMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> true
            else -> false
        }//Проверка на беззвучный режим

        mediaSession = MediaSessionCompat(this, "VolumeButtonObserver")
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    0,
                    0f
                ) //you simulate a player which plays something.
                .build()
        )

        val myVolumeProvider: VolumeProviderCompat = object : VolumeProviderCompat(
            VOLUME_CONTROL_RELATIVE,100, currentVolume) {
            override fun onAdjustVolume(direction: Int) {
                val ringerMode = audioManager.ringerMode
                if (!isSilentMode) {
                    VibratorSingleton.cancel()
//                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                }
                if (!isSilentMode && ringerMode != AudioManager.RINGER_MODE_VIBRATE){
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                }
            }

            override fun onSetVolumeTo(volume: Int) {
                super.onSetVolumeTo(volume)
            }
        }

        mediaSession?.apply {
            setPlaybackToRemote(myVolumeProvider)
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSilentMode) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, currentVolume, 0)
        }
        mediaSession?.release()
        mediaSession = null
    }
}


