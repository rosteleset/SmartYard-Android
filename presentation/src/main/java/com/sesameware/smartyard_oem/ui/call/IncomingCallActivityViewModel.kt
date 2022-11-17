package com.sesameware.smartyard_oem.ui.call

import androidx.lifecycle.MutableLiveData
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.FcmCallData
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class IncomingCallActivityViewModel(val preferenceStorage: PreferenceStorage) : GenericViewModel() {
    private lateinit var mFcmCallData: FcmCallData
    private var slideShowTimer: Timer? = null
    val imageStringData = MutableLiveData<Event<String>>()
    val eyeState = MutableLiveData<Boolean>()
    val connected = MutableLiveData<Event<Unit>>()
    val routeAudioTo = MutableLiveData<Boolean>()

    private fun playSlideShow(live: String) {
        slideShowTimer = fixedRateTimer("timer", false, 0, 1000) {
            Timber.d("debug_dmm slideshow tick")
            imageStringData.postValue(Event(live))
        }
    }

    fun routeAudioToValue(flag: Boolean) {
        routeAudioTo.value = (flag)
    }

    private fun stopSlideShow() {
        slideShowTimer?.cancel()
        slideShowTimer = null
    }

    fun switchStreamMode(on: Boolean) {
        if (on) {
            playSlideShow(mFcmCallData.live)
        } else {
            stopSlideShow()
            imageStringData.postValue(Event(mFcmCallData.image))
        }
    }

    override fun onCleared() {
        stopSlideShow()
        super.onCleared()
    }

    fun start(data: FcmCallData) {
        mFcmCallData = data
        switchStreamMode(false)
    }

    fun connectedСhangeStateUiAudioToSpeaker() {
        connected.value = Event(Unit)
    }
}
