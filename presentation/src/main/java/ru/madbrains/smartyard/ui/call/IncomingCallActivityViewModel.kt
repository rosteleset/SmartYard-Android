package ru.madbrains.smartyard.ui.call

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.MutableLiveData
import ru.madbrains.domain.model.FcmCallData
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class IncomingCallActivityViewModel : GenericViewModel() {
    private lateinit var mFcmCallData: FcmCallData
    private var slideShowTimer: Timer? = null
    val imageBitmapData = MutableLiveData<Event<Bitmap>>()
    val imageStringData = MutableLiveData<Event<String>>()
    val eyeState = MutableLiveData<Boolean>()
    val connected = MutableLiveData<Event<Unit>>()
    val routeAudioTo = MutableLiveData<Boolean>()

    private fun playSlideShow(live: String) {
        slideShowTimer = fixedRateTimer("timer", false, 0, 500) {
            Timber.d("debug_dmm slideshow tick")
            downloadImageTask(live)
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

    private fun downloadImageTask(vararg urls: String) {
        val urldisplay = urls[0]
        try {
            val inValue = java.net.URL(urldisplay).openStream()
            val result = BitmapFactory.decodeStream(inValue)
            imageBitmapData.postValue(Event(result))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start(data: FcmCallData) {
        mFcmCallData = data
        switchStreamMode(false)
    }

    fun connectedСhangeStateUiAudioToSpeaker() {
        connected.value = Event(Unit)
    }
}
