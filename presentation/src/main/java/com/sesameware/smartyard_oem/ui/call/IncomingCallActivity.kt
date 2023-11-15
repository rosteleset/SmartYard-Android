package com.sesameware.smartyard_oem.ui.call

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.linphone.core.RegistrationState
import com.sesameware.domain.model.FcmCallData
import com.sesameware.domain.utils.doDelayed
import com.sesameware.smartyard_oem.*
import com.sesameware.smartyard_oem.databinding.ActivityIncomingCallBinding
import com.sesameware.smartyard_oem.ui.showStandardAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber
import kotlin.math.min

class IncomingCallActivity : CommonActivity(), KoinComponent, SensorEventListener {
    private lateinit var binding: ActivityIncomingCallBinding

    //WebRTC staff
    private val rootEglBase: EglBase = EglBase.create()
    private var firstFrameRendered = false

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private val peerConnectionFactory by lazy {
        buildPeerConnectionFactory()
    }

    private val peerConnection by lazy {
        buildPeerConnection(object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Timber.d("debug_webrtc onSignalingChange")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Timber.d("debug_webrtc onConnectionChange: $p0")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Timber.d("debug_webrtc onIceConnectionReceivingChange: $p0")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Timber.d("debug_webrtc onIceGatheringChange: $p0")
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                Timber.d("debug_webrtc onIceCandidate: $p0")
                addIceCandidate(p0)
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                Timber.d("debug_webrtc onIceCandidatesRemoved: $p0")
            }

            override fun onAddStream(p0: MediaStream?) {
                Timber.d("debug_webrtc onAddStream: $p0")
                p0?.videoTracks?.get(0)?.addSink(binding.mWebRTCView)
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Timber.d("debug_webrtc onRemoveStream: $p0")
                p0?.videoTracks?.get(0)?.removeSink(binding.mWebRTCView)
            }

            override fun onDataChannel(p0: DataChannel?) {
                Timber.d("debug_webrtc onDataChannel: $p0")
            }

            override fun onRenegotiationNeeded() {
                Timber.d("debug_webrtc onRenegotiationNeeded")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                Timber.d("debug_webrtc onAddTrack: $p0 \n $p1")
            }
        })
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun buildPeerConnection(observer: PeerConnection.Observer) = peerConnectionFactory.createPeerConnection(
        listOf(PeerConnection.IceServer.builder(mFcmCallData.stun).createIceServer()),
        observer
    )

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun call() = peerConnection?.call()

    private fun PeerConnection.call() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Timber.d("debug_webrtc    onCreateSuccess    ${desc?.description}")

                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Timber.e("debug_webrtc onSetFailure: $p0")
                    }

                    override fun onSetSuccess() {
                        val body = RequestBody.create("application/sdp".toMediaTypeOrNull(), desc?.description ?: "")
                        Timber.d("debug_webrtc    ${body.contentType()}    ${body.contentLength()}")
                        val request = Request.Builder()
                            .url(mFcmCallData.webRtcVideoUrl)
                            .method("POST", body)
                            .build()
                        val httpClient = OkHttpClient.Builder().build()
                        try {
                            httpClient.newCall(request).execute().use { response ->
                                Timber.d("debug_webrtc     ${response.code}")
                                if (response.isSuccessful) {
                                    val sdpAnswer = SessionDescription(SessionDescription.Type.ANSWER, response.body!!.string())
                                    peerConnection?.setRemoteDescription(object : SdpObserver {
                                        override fun onCreateSuccess(p0: SessionDescription?) {
                                            Timber.d("debug_webrtc onCreateSuccessRemoteSession: Description $p0")
                                        }

                                        override fun onSetSuccess() {
                                            Timber.d("debug_webrtc onSetSuccessRemoteSession")
                                        }

                                        override fun onCreateFailure(p0: String?) {
                                            Timber.d("debug_webrtc onCreateFailure")
                                        }

                                        override fun onSetFailure(p0: String?) {
                                            Timber.e("debug_webrtc onSetFailure: $p0")
                                        }

                                    }, sdpAnswer)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e("$e")
                        }

                        Timber.d("debug_webrtc onSetSuccess")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Timber.d("debug_webrtc    onCreateSuccess: Description $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Timber.e("debug_webrtc onCreateFailure: $p0")
                    }
                }, desc)
            }

            override fun onSetSuccess() {
                Timber.d("debug_webrtc onSetSuccess")
            }

            override fun onSetFailure(p0: String?) {
                Timber.e("debug_webrtc onSetFailure: $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Timber.e("debug_webrtc onCreateFailure: $p0")
            }
        }, constraints)
    }

    private lateinit var mLinphone: LinphoneProvider
    private var mTryingToOpenDoor: Boolean = false
    override val mViewModel by viewModel<IncomingCallActivityViewModel>()
    private lateinit var mFcmCallData: FcmCallData
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private val SENSOR_SENSITIVITY = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mSensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        setupUi()

        val fcmData = intent.extras?.get(FCM_DATA) as FcmCallData?
        val provider = LinphoneService.instance?.provider
        if (provider != null && fcmData != null) {
            mLinphone = provider
            mFcmCallData = fcmData
            intentParse()
            observeChanges()
            checkAndRequestCallPermissions()
            mLinphone.setNativeVideoWindowId(binding.mVideoSip)

            mViewModel.start(mFcmCallData)
            binding.mImageViewWrap.clipToOutline = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }

            binding.mOpenButton.setOnClickListener { openDoor() }
            binding.mAnswerButton.setOnClickListener { answerCall() }
            if (mFcmCallData.image.isEmpty() && mFcmCallData.videoStream.isNotEmpty()) {
                mViewModel.eyeState.value = true
                binding.mPeekButton.setOnClickListener(null)
                binding.mPeekView.alpha = 0.0f
            } else {
                mViewModel.eyeState.value = LinphoneService.instance?.provider?.fcmData?.eyeState == true
                binding.mPeekButton.setOnClickListener {
                    mViewModel.eyeState.value = !binding.mPeekButton.isChecked
                    mLinphone.stopRinging()
                }
            }
            binding.mHangUpButton.setOnClickListener { hangUp() }
        } else {
            finish()
        }

        var useSpeaker = false
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            useSpeaker = true
        } else {
            //включение громкой связи, если выставлен флаг в настройках домофона
            if (fcmData != null) {
                if (mViewModel.preferenceStorage.addressOptions.getOption(fcmData.flatId).isSpeaker == true) {
                    useSpeaker = true
                }
            }
        }
        mViewModel.routeAudioToValue(useSpeaker)

        //WebRTC
        if (mFcmCallData.videoStream.isNotEmpty()) {
            initPeerConnectionFactory(application)
            binding.mWebRTCView.run {
                setEnableHardwareScaler(true)
                init(rootEglBase.eglBaseContext, object : RendererEvents {
                    override fun onFirstFrameRendered() {
                        firstFrameRendered = true
                        if (mLinphone.isConnected() || mFcmCallData.image.isEmpty() || mViewModel.eyeState.value == true) {
                            alpha = 1.0f
                            binding.mPeekView.visibility = View.INVISIBLE
                        }
                    }

                    override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                        val w = binding.mImageViewWrap.width
                        val h = binding.mImageViewWrap.height
                        if (w > 0 && h > 0 && p0 > 0 && p1 > 0) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                val k = min(h.toFloat() / p1.toFloat(), w.toFloat() / p0.toFloat())
                                binding.mWebRTCView.layoutParams?.let { lp ->
                                    lp.width = (k * p0).toInt()
                                    lp.height = (k * p1).toInt()
                                }
                            }
                        }
                    }
                })
                alpha = 0.0f
                visibility = View.VISIBLE
                bringToFront()
            }
            call()
        }
    }

    private fun setupUi() {
        binding.ivFullscreenMinimalize.setOnClickListener {
            requestedOrientation =
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
        }

        binding.mSpeakerButton.setOnClickListener {
            mViewModel.routeAudioToValue(!binding.mSpeakerButton.isSelected)
        }
    }

    private fun hangUp() {
        LinphoneService.instance?.stopSelf()
    }

    private fun openDoor() {
        if (mLinphone.isConnected()) {
            binding.mAnswerButton.setText(R.string.connecting)
            mLinphone.sendDtmf()
        } else {
            mTryingToOpenDoor = true
            mLinphone.acceptCallForDoor()
        }
    }

    private fun observeChanges() {
        mLinphone.registrationState.observe(this) { observeRegistrationState(it) }
        mLinphone.callState.observe(this) { observeCallState(it) }
        mLinphone.dtmfIsSent.observe(this) { setDoorState(it) }
        mLinphone.finishCallActivity.observe(
            this,
            EventObserver {
                finish()
            }
        )
        mViewModel.localErrorsSink.observe(
            this,
            EventObserver { error ->
                showStandardAlert(this, error.status.messageId)
            }
        )
        mViewModel.eyeState.observe(
            this
        ) { boolean ->
            setPeek(boolean)
        }
        mViewModel.imageStringData.observe(
            this,
            EventObserver { string ->
                if (string.isEmpty()) {
                    return@EventObserver
                }

                Glide.with(binding.mPeekView)
                    .asBitmap()
                    .load(string)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            binding.mPeekView.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        )

        mViewModel.connected.observe(
            this,
            EventObserver {
                binding.mAnswerButton.isVisible = false
                binding.mSpeakerButton.isVisible = true
            }
        )

        mViewModel.routeAudioTo.observe(
            this
        ) {
            if (it) {
                mLinphone.mAudioManager.routeAudioToSpeaker()
                binding.mSpeakerButton.isSelected = true
            } else {
                mLinphone.mAudioManager.routeAudioToEarPiece()
                binding.mSpeakerButton.isSelected = false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { intentParse() }
    }

    private fun resetView(data: FcmCallData) {
        mLinphone.reset()
        setDoorState(false)
        binding.mStatusText.text = data.callerId
    }

    private fun intentParse() {
        resetView(mFcmCallData)
    }

    private fun setDoorState(opened: Boolean) {
        if (opened) {
            doDelayed(
                {
                    mLinphone.disconnect()
                    hangUp()
                },
                3000
            )
        }
        binding.mOpenedButton.show(opened, true)
        binding.mHangUpButton.show(!opened)
        binding.mOpenButton.show(!opened)
    }

    private fun answerCall() {
        if (!binding.mAnswerButton.isSelected && mLinphone.dtmfIsSent.value != true) {
            binding.mAnswerButton.isSelected = true
            mLinphone.acceptCall()
        }
    }

    private fun setConnectedState(connected: Boolean) {
        if (connected) {
            setPeek(false)
            binding.mPeekButton.setOnClickListener(null)
        } else
            if (mFcmCallData.image.isNotEmpty()) {
                binding.mPeekButton.setOnClickListener {
                    mViewModel.eyeState.value = !binding.mPeekButton.isChecked
                    mLinphone.stopRinging()
                }
            }

        toggleCallClock(connected)
        if (mFcmCallData.videoStream.isEmpty()) {
            binding.mVideoSip.show(connected)
            binding.mVideoSip.bringToFront()
        } else {
            if (firstFrameRendered) {
                binding.mWebRTCView.alpha = 1.0f
                binding.mPeekView.visibility = View.INVISIBLE
            }
        }
        binding.mHangUpButton.setText(if (connected) R.string.reject else R.string.ignore)
        binding.mAnswerButton.setText(if (connected) R.string.connected else R.string.answer)
        binding.mAnswerButton.isSelected = connected
        if (connected) {
            mViewModel.connectedChangeStateUiAudioToSpeaker()
        }
    }

    private fun setPeek(peek: Boolean) {
        setTitleState(mLinphone.isConnected(), peek)
        mViewModel.switchStreamMode(peek)
        binding.mPeekButton.isChecked = peek
        if (peek) {
            if (firstFrameRendered) {
                binding.mWebRTCView.alpha = 1.0f
                binding.mPeekView.visibility = View.INVISIBLE
            } else {
                binding.mPeekView.visibility = View.VISIBLE
            }
        } else if (!mLinphone.isConnected() && mFcmCallData.image.isNotEmpty()) {
            binding.mWebRTCView.alpha = 0.0f
            binding.mPeekView.visibility = View.VISIBLE
        }
    }

    private fun setTitleState(connected: Boolean, peek: Boolean) {
        val text = if (!connected) {
            if (peek) R.string.call_peek_on else R.string.call_on_domophone
        } else R.string.call_talk
        binding.mTitle.setText(text)
    }

    private fun toggleCallClock(on: Boolean) {
        binding.mStatusText.show(!on)
        binding.mCallTimer.show(on)
        if (on) {
            binding.mCallTimer.base = SystemClock.elapsedRealtime() - 1000 * (mLinphone.getCallDuration())
            binding.mCallTimer.start()
        } else {
            binding.mCallTimer.stop()
        }
    }

    private fun observeRegistrationState(it: CRegistrationState) {
        it.run {
            val text = when (state) {
                RegistrationState.Ok -> R.string.answer
                RegistrationState.Failed -> R.string.error
                RegistrationState.Progress -> R.string.connecting
                else -> R.string.answer
            }
            binding.mAnswerButton.setText(text)
            if (state == RegistrationState.None) {
                binding.mAnswerButton.isSelected = false
            }
        }
    }

    private fun observeCallState(it: CCallState) {
        it.run {
            when (state) {
                CallStateSimple.INCOMING -> {
                }
                CallStateSimple.OTHER_CONNECTED -> {
                    setConnectedState(true)
                }
                CallStateSimple.CONNECTED -> {
                    if (mTryingToOpenDoor) {
                        binding.mAnswerButton.setText(R.string.connecting)
                        mLinphone.sendDtmf()
                    } else {
                        setConnectedState(true)
                    }
                }
                CallStateSimple.CONNECTING -> {
                    binding.mAnswerButton.setText(R.string.connecting)
                }
                CallStateSimple.ERROR -> {
                    binding.mAnswerButton.setText(R.string.error)
                }
                CallStateSimple.END -> {
                }
                CallStateSimple.IDLE -> {
                    setConnectedState(false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        mSensorManager?.unregisterListener(this)
        mLinphone.mAudioManager.routeAudioToEarPiece()
        LinphoneService.instance?.provider?.fcmData?.eyeState = binding.mPeekButton.isChecked
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mFcmCallData.videoStream.isNotEmpty()) {
            peerConnection?.close()
            peerConnection?.dispose()
            binding.mWebRTCView.release()
            rootEglBase.release()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                mViewModel.routeAudioToValue(false)
            }
        }
    }

    private fun checkAndRequestCallPermissions() {
        val permissionsList = ArrayList<String>()
        val recordAudio =
            packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, packageName)

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsList.size > 0) {
            val permissions: Array<String> = permissionsList.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }

    companion object {
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        const val FCM_DATA = "FCM_DATA"
    }
}
