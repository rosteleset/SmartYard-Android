package ru.madbrains.smartyard.ui.call

import android.content.Context
import android.media.AudioManager

/**
 * @author Nail Shakurov
 * Created on 23.07.2020.
 */
class AndroidAudioManager(var context: Context) {

    private var mAudioManager: AudioManager? = null

    init {
        mAudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun routeAudioToEarPiece() {
        routeAudioToSpeakerHelper(false)
    }

    fun routeAudioToSpeaker() {
        routeAudioToSpeakerHelper(true)
    }

    private fun routeAudioToSpeakerHelper(speakerOn: Boolean) {
        mAudioManager?.isSpeakerphoneOn = speakerOn
    }
}
