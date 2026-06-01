package com.sesameware.smartyard_oem.ui.call

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.doDelayed
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.CCallState
import com.sesameware.smartyard_oem.CRegistrationState
import com.sesameware.smartyard_oem.CallStateSimple
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.LinphoneProvider
import com.sesameware.smartyard_oem.LinphoneService
import com.sesameware.smartyard_oem.MessagingService.Companion.CALL_STUN
import com.sesameware.smartyard_oem.MessagingService.Companion.CALL_STUN_TRANSPORT
import com.sesameware.smartyard_oem.MessagingService.Companion.CALL_TURN_PASSWORD
import com.sesameware.smartyard_oem.MessagingService.Companion.CALL_TURN_USERNAME
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityIncomingCallBinding
import com.sesameware.smartyard_oem.show
import com.sesameware.smartyard_oem.ui.showStandardAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.linphone.core.Call
import org.linphone.core.RegistrationState
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.EglRenderer
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
import java.util.concurrent.TimeUnit
import kotlin.math.min

class IncomingCallActivity : CommonActivity(), KoinComponent, SensorEventListener {
    private lateinit var binding: ActivityIncomingCallBinding

    // WebRTC staff
    private val rootEglBase: EglBase by inject()
    private var webRtcFirstFrameRendered = false
    private var isWebRTCStopped = true
    private var peerConnection: PeerConnection? = null
    private var frameBitmap: Bitmap?  = null

    // Listener to maintain the last frame from WebRTC
    private var webRTCListener = EglRenderer.FrameListener {bitmap: Bitmap? ->
        frameBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    private val peerConnectionFactory: PeerConnectionFactory by inject()

    private fun buildPeerConnection(observer: PeerConnection.Observer) =
        peerConnectionFactory.createPeerConnection(
            listOf(PeerConnection.IceServer.builder(mPushCallData.stun).createIceServer()),
            observer
        )

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun createConnection() {
        peerConnection = buildPeerConnection(object : PeerConnection.Observer {
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
                p0?.videoTracks?.firstOrNull()?.addSink(binding.mWebRTCView)
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Timber.d("debug_webrtc onRemoveStream: $p0")
                p0?.videoTracks?.firstOrNull()?.removeSink(binding.mWebRTCView)
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
        peerConnection?.createConnection()
    }

    private fun PeerConnection.createConnection() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Timber.d("debug_webrtc    onCreateSuccess    ${desc?.description}")

                setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        val body = RequestBody.create("application/sdp".toMediaTypeOrNull(), desc?.description ?: "")
                        Timber.d("debug_webrtc    server URL ${mPushCallData.webRtcVideoUrl}")
                        Timber.d("debug_webrtc    ${body.contentType()}    ${body.contentLength()}")
                        val request = Request.Builder()
                            .url(mPushCallData.webRtcVideoUrl)
                            .post(body)
                            .build()
                        val httpClient = OkHttpClient.Builder()
                            .callTimeout(5, TimeUnit.SECONDS)
                            .build()
                        try {
                            httpClient.newCall(request).execute().use { response ->
                                Timber.d("debug_webrtc    response code ${response.code}")
                                if (response.isSuccessful) {
                                    val sdpAnswer = SessionDescription(SessionDescription.Type.ANSWER, response.body!!.string())
                                    setRemoteDescription(object : SimpleSdpObserver() {
                                        override fun onSetSuccess() {
                                            Timber.d("debug_webrtc onSetSuccessRemoteSession")
                                        }
                                    }, sdpAnswer)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.d("debug_webrtc $e")
                        }
                        Timber.d("debug_webrtc onSetSuccess")
                    }
                }, desc)
            }
        }, constraints)
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {
            Timber.e("debug_webrtc onCreateFailure: $p0")
        }
        override fun onSetFailure(p0: String?) {
            Timber.e("debug_webrtc onSetFailure: $p0")
        }
    }

    private var mLinphone: LinphoneProvider? = null
    private var mTryingToOpenDoor: Boolean = false
    override val mViewModel by viewModel<IncomingCallActivityViewModel>()
    private lateinit var mPushCallData: PushCallData
    private val hasWebRTC: Boolean
        get() = ::mPushCallData.isInitialized && mPushCallData.videoStream.isNotEmpty()
    private var hasSnapshot = true
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private var useSpeaker = false

    private fun startWebRTC() {
        if (!isWebRTCStopped) {
            return
        }

        setupWebRtcView()
        binding.mWebRTCView.addFrameListener(webRTCListener, 1f)
        createConnection()
        isWebRTCStopped = false
    }

    private fun setupWebRtcView() {
        binding.mWebRTCView.run {
            setEnableHardwareScaler(true)
            init(rootEglBase.eglBaseContext, object : RendererEvents {
                override fun onFirstFrameRendered() {
                    Timber.d("debug_webrtc    onFirstFrameRendered")
                    webRtcFirstFrameRendered = true
                    Timber.d("debug_webrtc    make mWebRTCView visible onFirstFrameRendered")
                    alpha = 1.0f
                    Timber.d("debug_webrtc    make mPeekImageView invisible onFirstFrameRendered")
                    binding.mPeekImageView.visibility = View.INVISIBLE
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    Timber.d("debug_webrtc    onFrameResolutionChanged")
                    val w = binding.mImageViewWrap.width
                    val h = binding.mImageViewWrap.height
                    if (w > 0 && h > 0 && videoWidth > 0 && videoHeight > 0) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            val scale = min(h.toFloat() / videoHeight.toFloat(), w.toFloat() / videoWidth.toFloat())
                            layoutParams?.let { lp ->
                                lp.width = (scale * videoWidth).toInt()
                                lp.height = (scale * videoHeight).toInt()
                            }
                        }
                    }
                }
            })
            alpha = 0.0f
            visibility = View.VISIBLE
            bringToFront()
        }
        Timber.d("debug_webrtc    make mWebRTCView invisible on initWebRTC")
    }

    private fun stopWebRTC() {
        if (!isWebRTCStopped && hasWebRTC) {
            if (binding.mPeekImageView.isLaidOut) {
                Timber.d("debug_webrtc    copy last frame")
                binding.mPeekImageView.setImageBitmap(frameBitmap)
            }
            Timber.d("debug_webrtc    make mWebRTCView invisible stopWebRtcConnection")
            binding.mWebRTCView.alpha = 0.0f
            binding.mWebRTCView.removeFrameListener(webRTCListener)
            binding.mWebRTCView.release()

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            isWebRTCStopped = true

            webRtcFirstFrameRendered = false

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        Timber.d("debug_dmm    onCreate")

        mSensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        setupUi()
        initClickListeners()
        enableCallButtons(false)

        @Suppress("DEPRECATION") val fcmData = intent.extras?.get(PUSH_DATA) as PushCallData?
        waitForLinServiceAndRun(fcmData) {
            mLinphone = LinphoneService.instance?.provider
            if (mLinphone != null && fcmData != null) {
                if (LinphoneService.instance?.connectionStarted == false) {
                    mLinphone?.startConnection(fcmData)
                }
                mPushCallData = fcmData
                resetView(mPushCallData)
                checkAndRequestCallPermissions()
                mLinphone?.setNativeVideoWindowId(binding.mVideoSip)

                mViewModel.start(mPushCallData)
                binding.mImageViewWrap.clipToOutline = true
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                }

                observeChanges()
                if (LinphoneService.instance?.isCallOk == true) {
                    enableCallButtons(true)
                }
                setConnectedState(mLinphone?.isConnected() == true)
            } else {
                Timber.d("debug_dmm    call finishAndRemoveTask")
                finishAndRemoveTask()
            }

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                useSpeaker = true
            } else {
                //включение громкой связи, если выставлен флаг в настройках домофона
                if (fcmData != null) {
                    if (mViewModel.mPreferenceStorage.addressOptions.getOption(fcmData.flatId).isSpeaker == true) {
                        useSpeaker = true
                    }
                }
            }
        }
    }

    private fun setupUi() {

        fragmentHasHeader = true
        lightNavBar = true

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    private fun enableCallButtons(isEnabled: Boolean) {
        with(binding) {
            mPeepholeButton.isEnabled = isEnabled
            mAnswerButton.isEnabled = isEnabled
            mAnswerButtonSupport.isEnabled = isEnabled
            mSpeakerButton.isEnabled = isEnabled
            mHangUpButton.isEnabled = isEnabled
            mHangUpButtonSupport.isEnabled = isEnabled
            mOpenedButton.isEnabled = isEnabled
            mOpenButton.isEnabled = isEnabled
            pbIncomingCall.isVisible = !isEnabled
        }

        if (isEnabled) {
            val ff = hasWebRTC && (mLinphone?.isVideoCall() == false || mPushCallData.image.isEmpty())
            Timber.d("debug_dmm  enableCallButtons set eyeState = $ff")
            LinphoneService.instance?.provider?.pushCallData?.eyeState = ff
            mViewModel.eyeState.value = ff
        }
    }

    private fun initClickListeners() {
        with(binding) {
            ivFullscreenMinimalize.setOnClickListener {
                cancelNotification()
                requestedOrientation =
                    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
            }

            mSpeakerButton.setOnClickListener {
                cancelNotification()
                mViewModel.routeAudioToValue(!mSpeakerButton.isSelected)
            }

            mOpenButton.setOnClickListener {
                cancelNotification()
                openDoor()
            }

            val onAnswerClick = View.OnClickListener {
                cancelNotification()
                answerCall()
            }
            mAnswerButton.setOnClickListener(onAnswerClick)
            mAnswerButtonSupport.setOnClickListener(onAnswerClick)

            mPeepholeButton.setOnClickListener {
                if (mLinphone?.isConnected() == false) {
                    cancelNotification()
                    mViewModel.eyeState.value = !mPeepholeButton.isChecked
                }
            }

            val onHangUpClick = View.OnClickListener {
                cancelNotification()
                hangUp()
            }
            mHangUpButton.setOnClickListener(onHangUpClick)
            mHangUpButtonSupport.setOnClickListener(onHangUpClick)
        }
    }

    private fun waitForLinServiceAndRun(fcmCallData: PushCallData?, listener: listenerGeneric<LinphoneProvider>) {
        lifecycleScope.launch(Dispatchers.IO) {
            var doStartService = false
            if (!LinphoneService.isReady()) {
                doStartService = true
                startService(
                    Intent().setClass(this@IncomingCallActivity, LinphoneService::class.java).also { intent ->
                        if (fcmCallData?.stun?.isNotEmpty() == true) {
                            intent.putExtra(CALL_STUN, fcmCallData.stun)
                            intent.putExtra(CALL_STUN_TRANSPORT, fcmCallData.stun_transport ?: "udp")
                            intent.putExtra(CALL_TURN_USERNAME, fcmCallData.extension)
                            intent.putExtra(CALL_TURN_PASSWORD, fcmCallData.pass)
                        }
                    }
                )
            }
            val timestamp = System.currentTimeMillis()
            var failed = false
            while (!LinphoneService.isReady()) {
                if (System.currentTimeMillis() - timestamp > WAIT_FOR_LINPHONE) {
                    failed = true
                    break
                }
                delay(30L)
            }
            withContext(Dispatchers.Main) {
                if (failed) {
                    processFailedCall()
                } else {
                    if (doStartService) {
                        Timber.d("debug_dmm  Linphone service has started...")
                    }
                    LinphoneService.instance?.provider?.let {
                        listener(it)
                    }
                }
            }
        }
    }

    private fun hangUp() {
        LinphoneService.instance?.stopSelf()
    }

    private fun openDoor() {
        if (mLinphone?.isConnected() == true) {
            updateAnswerButtonsText(R.string.connecting)
            mLinphone?.sendDtmf()
        } else {
            mTryingToOpenDoor = true
            mLinphone?.acceptCallForDoor()
        }
    }

    private fun observeChanges() {
        mLinphone?.registrationState?.observe(this) { observeRegistrationState(it) }
        mLinphone?.callState?.observe(this) { observeCallState(it) }
        mLinphone?.dtmfIsSent?.observe(this) { setDoorState(it) }
        mLinphone?.finishCallActivity?.observe(
            this,
            EventObserver {
                Timber.d("debug_dmm    finishCallActivity")
                finishAndRemoveTask()
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
            Timber.d("debug_dmm  eyeState observer call enablePeepholeVideo($boolean)")
            enablePeepholeVideo(boolean)
        }
        mViewModel.imageStringData.observe(
            this,
            EventObserver { string ->
                if (string.isEmpty() || !hasSnapshot) {
                    return@EventObserver
                }
                Timber.d("debug_dmm  observe imageStringData url=$string")
                Glide.with(this)
                    .asBitmap()
                    .load(string)
                    .timeout(GLIDE_TIMEOUT_MS.toInt())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Timber.d("debug_webrtc    snapshot failed")
                            Timber.d("debug_webrtc    make mPeekImageView invisible imageStringData")
                            binding.mPeekImageView.isVisible = false
                            if (hasWebRTC) {
                                hasSnapshot = false
                                Timber.d("debug_dmm  imageStringData.observe set eyeState = true")
                                LinphoneService.instance?.provider?.pushCallData?.eyeState = true
                                mViewModel.eyeState.value = true
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap?>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Timber.d("debug_webrtc    make mPeekImageView visible onResourceReady")
                            binding.mPeekImageView.isVisible = true
                            return false
                        }
                    })
                    .into(binding.mPeekImageView)
            }
        )

        mViewModel.connected.observe(
            this,
            EventObserver {
                binding.mAnswerButton.isVisible = false
                binding.mAnswerButtonSupport.isVisible = false
                binding.mSpeakerButton.isVisible = true
            }
        )

        mViewModel.routeAudioTo.observe(
            this
        ) {
            if (it) {
                mLinphone?.routeAudioToSpeaker()
                binding.mSpeakerButton.isSelected = true

                /*Timber.d("debug_dmm    pause call;    state = ${mLinphone?.core?.currentCall?.state}")
                mLinphone?.core?.currentCall?.pause()*/
            } else {
                mLinphone?.routeAudioToEarpiece()
                binding.mSpeakerButton.isSelected = false
            }
        }
    }

    private fun cancelNotification() {
        Timber.d("debug_dmm    cancelNotification")
        intent.extras?.getInt(NOTIFICATION_ID)?.let {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(it)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        @Suppress("DEPRECATION") val fcmData = intent.extras?.get(PUSH_DATA) as PushCallData?
        fcmData?.let {
            mPushCallData = it
            resetView(mPushCallData)
        }
    }

    private fun resetView(data: PushCallData) {
        mLinphone?.reset()
        setDoorState(false)
        binding.mStatusText.text = data.callerId
        if (data.isSupport) {
            binding.mPanel.visibility = View.INVISIBLE
            binding.mSupportRow?.visibility = View.VISIBLE
        } else {
            binding.mPanel.visibility = View.VISIBLE
            binding.mSupportRow?.visibility = View.INVISIBLE
        }
    }

    private fun setDoorState(opened: Boolean) {
        if (opened) {
            doDelayed(
                {
                    mLinphone?.disconnect()
                    hangUp()
                },
                DOOR_OPEN_DELAY_MS
            )
        }
        binding.mOpenedButton.show(opened, true)
        binding.mHangUpButton.show(!opened)
        binding.mHangUpButtonSupport.show(!opened)
        if (this::mPushCallData.isInitialized && mPushCallData.dtmf.isNotEmpty()) {
            binding.mOpenButton.show(!opened)
        }
    }

    private fun updateAnswerButtonsText(resId: Int) {
        binding.mAnswerButton.setText(resId)
        binding.mAnswerButtonSupport.setText(resId)
    }

    private fun updateHangUpButtonsText(resId: Int) {
        binding.mHangUpButton.setText(resId)
        binding.mHangUpButtonSupport.setText(resId)
    }

    private fun setAnswerButtonsSelected(isSelected: Boolean) {
        binding.mAnswerButton.isSelected = isSelected
        binding.mAnswerButtonSupport.isSelected = isSelected
    }

    private fun answerCall() {
        Timber.d("debug_dmm    answerCall")
        if (!binding.mAnswerButton.isSelected && mLinphone?.dtmfIsSent?.value == false) {
            setAnswerButtonsSelected(true)
            mLinphone?.acceptCall()
        }
    }

    private fun setConnectedState(connected: Boolean) {
        Timber.d("debug_dmm    call setConnectedState connected=$connected")
        if (connected) {
            val ff = hasWebRTC && (mLinphone?.isVideoCall() == false)
            Timber.d("debug_dmm  setConnectedState set eyeState = $ff")
            LinphoneService.instance?.provider?.pushCallData?.eyeState = ff
            mViewModel.eyeState.value = ff
            if (mLinphone?.isVideoCall() == true) {
                Timber.d("debug_webrtc    answer the call with video in SIP")
                binding.mVideoSip.show(true)
                binding.mVideoSip.bringToFront()
            } else if (hasWebRTC) {
                Timber.d("debug_webrtc    answer the call with WebRTC video")
            }
        }

        switchCallClock(connected)
        updateHangUpButtonsText(if (connected) R.string.reject else R.string.ignore)
        updateAnswerButtonsText(if (connected) R.string.connected else R.string.answer)
        setAnswerButtonsSelected(connected)

        if (connected) {
            mViewModel.connectedChangeStateUiAudioToSpeaker()
        }
    }

    private fun enablePeepholeVideo(isEnabled: Boolean) {
        Timber.d("debug_dmm    call enablePeepholeVideo isEnabled=$isEnabled")
        val text = when {
            mLinphone?.isConnected() == true -> getString(R.string.call_talk)
            isEnabled -> getString(R.string.call_peek_on)
            mPushCallData.isSupport -> mPushCallData.title
            else -> getString(R.string.call_on_domophone)
        }
        binding.mTitle.text = text
        mViewModel.setSlideShowEnabled(!hasWebRTC && isEnabled)
        binding.mPeepholeButton.isChecked = isEnabled

        if (hasWebRTC) {
            if (isEnabled) {
                startWebRTC()
            } else {
                Timber.d("debug_webrtc    make mPeekImageView visible enablePeepholeVideo")
                binding.mPeekImageView.visibility = View.VISIBLE

                stopWebRTC()
                Timber.d("debug_webrtc    make mWebRTCView invisible enablePeepholeVideo")
                binding.mWebRTCView.alpha = 0.0f
            }
        }
    }

    private fun switchCallClock(on: Boolean) {
        binding.mStatusText.show(!on)
        binding.mCallTimer.show(on)
        if (on) {
            binding.mCallTimer.base = SystemClock.elapsedRealtime() - 1000 * (mLinphone?.getCallDuration() ?: 0)
            binding.mCallTimer.start()
        } else {
            binding.mCallTimer.stop()
        }
    }

    private fun observeRegistrationState(it: CRegistrationState) {
        it.run {
            val textResId = when (state) {
                RegistrationState.Ok -> R.string.answer
                RegistrationState.Failed -> R.string.error
                RegistrationState.Progress -> R.string.connecting
                else -> R.string.answer
            }
            updateAnswerButtonsText(textResId)
            if (state == RegistrationState.None) {
                setAnswerButtonsSelected(false)
            }
            if (it.state == RegistrationState.Failed) {
                processFailedCall()
            }
        }
    }

    private fun observeCallState(it: CCallState) {
        Timber.d("debug_dmm    call observeCallState")
        it.run {
            when (state) {
                CallStateSimple.INCOMING -> {
                    Timber.d("debug_dmm  enable call buttons")
                    enableCallButtons(true)
                    mViewModel.routeAudioToValue(useSpeaker)
                }
                CallStateSimple.OTHER_CONNECTED -> {
                    //setConnectedState(true)
                }
                CallStateSimple.CONNECTED -> {
                }
                CallStateSimple.CONNECTING -> {
                    updateAnswerButtonsText(R.string.connecting)
                }
                CallStateSimple.ERROR -> {
                    updateAnswerButtonsText(R.string.error)
                    processFailedCall()
                }
                CallStateSimple.END -> {
                    finishAndRemoveTask()
                }
                CallStateSimple.IDLE -> {
                    setConnectedState(false)
                }
                CallStateSimple.STREAMS_RUNNING -> {
                    if (mTryingToOpenDoor) {
                        updateAnswerButtonsText(R.string.connecting)
                        mLinphone?.sendDtmf()
                    } else {
                        setConnectedState(true)
                    }
                }
                CallStateSimple.PAUSED -> {
                }
            }
        }
    }

    override fun onResume() {
        Timber.d("debug_dmm    onResume")

        super.onResume()
        mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)

        LinphoneService.instance?.mCore?.calls?.let { calls ->
            if (calls.size > 0) {
                if (calls[0].state == Call.State.Pausing || calls[0].state == Call.State.Paused) {
                    Timber.d("debug_dmm    resume paused call")
                    calls[0].resume()
                    mViewModel.routeAudioTo.value?.let { v ->
                        mViewModel.routeAudioToValue(v)
                    }
                }
            }
        }
    }

    override fun onPause() {
        Timber.d("debug_dmm    onPause")

        super.onPause()
        mSensorManager?.unregisterListener(this)
        mLinphone?.routeAudioToEarpiece()
        LinphoneService.instance?.provider?.pushCallData?.eyeState = binding.mPeepholeButton.isChecked

        //mLinphone?.core?.currentCall?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("debug_dmm    onDestroy")
        cancelNotification()
        stopWebRTC()
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
        if (permissionsList.isNotEmpty()) {
            val permissions: Array<String> = permissionsList.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }

    private fun processFailedCall() {
        Timber.d("debug_dmm    call processFailedCall")

        LinphoneService.instance?.stopSelf()
        binding.pbIncomingCall.isVisible = false

        Timber.d("debug_dmm    call finishAndRemoveTask")
        finishAndRemoveTask()
    }

    companion object {
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        const val PUSH_DATA = "PUSH_DATA"
        const val WAIT_FOR_LINPHONE = 10_000
        const val SENSOR_SENSITIVITY = 4
        private const val GLIDE_TIMEOUT_MS = 5_000L
        private const val DOOR_OPEN_DELAY_MS = 3_000L
    }
}
