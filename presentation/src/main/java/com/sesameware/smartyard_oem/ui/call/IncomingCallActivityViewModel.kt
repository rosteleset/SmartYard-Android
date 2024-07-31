package com.sesameware.smartyard_oem.ui.call

import androidx.lifecycle.MutableLiveData
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.PushCallData
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class IncomingCallActivityViewModel(
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    private lateinit var mPushCallData: PushCallData
    private var slideShowTimer: Timer? = null
    val imageStringData = MutableLiveData<Event<String>>()
    val eyeState = MutableLiveData(false)
    val connected = MutableLiveData<Event<Unit>>()
    val routeAudioTo = MutableLiveData<Boolean>()

    private fun playSlideShow(live: String) {
        if (live.isEmpty()) {
            return
        }

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

    fun setSlideShowEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            if (mPushCallData.videoStream.isEmpty()) {
                playSlideShow(mPushCallData.live)
            }
        } else {
            stopSlideShow()
            imageStringData.postValue(Event(mPushCallData.image))
        }
    }

    override fun onCleared() {
        stopSlideShow()
        super.onCleared()
    }

    fun start(data: PushCallData) {
        mPushCallData = data
        setSlideShowEnabled(false)
    }

    fun connectedChangeStateUiAudioToSpeaker() {
        connected.value = Event(Unit)
    }
}
