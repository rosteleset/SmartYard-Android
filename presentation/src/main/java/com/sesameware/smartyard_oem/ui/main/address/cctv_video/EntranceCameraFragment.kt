package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import com.sesameware.smartyard_oem.databinding.FragmentEntranceCameraBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min

class EntranceCameraFragment : Fragment() {
    private var _binding: FragmentEntranceCameraBinding? = null
    private val binding get() = _binding!!

    private val args: EntranceCameraFragmentArgs by navArgs()
    private val viewModel by sharedViewModel<AddressViewModel>()

    private val rootEglBase: EglBase by inject()
    private var peerConnection: PeerConnection? = null
    private val peerConnectionFactory: PeerConnectionFactory by inject()
    private var videoTrack: org.webrtc.VideoTrack? = null
    private var hlsPlayer: BaseCCTVPlayer? = null
    private var webRtcStarted = false
    private var currentWhepUrl: String? = null
    private var isOfferSent = false

    /**
     * Проверка на проблемные бюджетные устройства и 32-битные системы (архитектура arm-v7).
     * На таких девайсах аппаратный H264 декодер в WebRTC вызывает SIGILL в драйверах GPU.
     */
    private fun shouldForceSoftwareDecoder(): Boolean {
        val is32Bit = Build.SUPPORTED_64_BIT_ABIS.isEmpty()

        // Trouble devices
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val isBuggyDevice = manufacturer.contains("samsung", ignoreCase = true) &&
                (model.contains("A13", ignoreCase = true) ||
                        model.contains("A12", ignoreCase = true) ||
                        model.contains("A03", ignoreCase = true) ||
                        model.contains("A04", ignoreCase = true))

        return is32Bit || isBuggyDevice
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntranceCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterFullscreen()
        bindViews()
        startPlayback()
    }

    private fun bindViews() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tbOpen.setOnClickListener {
            viewModel.openDoor(args.lock)
            binding.tbOpen.isClickable = false
            binding.root.postDelayed(
                {
                    binding.tbOpen.isChecked = false
                    binding.tbOpen.isClickable = true
                },
                3000
            )
        }

        Glide.with(binding.ivPreview)
            .load(args.entranceCamera.previewUrl)
            .into(binding.ivPreview)
    }

    private fun startPlayback() {
        binding.progressBar.isVisible = true
        lifecycleScope.launch {
            val isWhepAvailable = args.entranceCamera.whepUrl.isNotBlank() &&
                    viewModel.isWhepAvailable(args.entranceCamera.whepUrl)
            if (isWhepAvailable) {
                startWebRtc(args.entranceCamera.whepUrl)
            } else {
                startHls(args.entranceCamera.hlsUrl)
            }
        }
    }

    private var webRtcVideoWidth: Int = 0
    private var webRtcVideoHeight: Int = 0
    private fun startWebRtc(whepUrl: String) {
        currentWhepUrl = whepUrl

        binding.webRtcView.run {
            setEnableHardwareScaler(true)

            init(rootEglBase.eglBaseContext, object : RendererEvents {
                override fun onFirstFrameRendered() {
                    post {
                        binding.progressBar.isVisible = false
                        binding.ivPreview.isVisible = false
                        alpha = 1.0f
                    }
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    post {
                        webRtcVideoWidth = videoWidth
                        webRtcVideoHeight = videoHeight
                        fitWebRtcView(videoWidth, videoHeight)
                    }
                }
            })
            alpha = 0.0f
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                post { fitWebRtcView(webRtcVideoWidth, webRtcVideoHeight) }
            }
        }

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        )
        Timber.d("debug_webrtc creating peer connection")
        peerConnection = peerConnectionFactory.createPeerConnection(
            iceServers,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    Timber.d("debug_webrtc onSignalingChange: $state")
                }
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Timber.d("debug_webrtc onIceConnectionChange: $state")
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Timber.d("debug_webrtc onIceConnectionReceivingChange: $receiving")
                }
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Timber.d("debug_webrtc onIceGatheringChange: $state")
                    if (state == PeerConnection.IceGatheringState.COMPLETE) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            checkAndSendOffer()
                        }
                    }
                }
                override fun onIceCandidate(candidate: IceCandidate?) {
                    Timber.d("debug_webrtc onIceCandidate: $candidate")
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: MediaStream?) {
                    Timber.d("debug_webrtc onAddStream")
                    val track = stream?.videoTracks?.firstOrNull()
                    videoTrack = track
                    track?.addSink(binding.webRtcView)
                }
                override fun onRemoveStream(stream: MediaStream?) {
                    Timber.d("debug_webrtc onRemoveStream")
                    stream?.videoTracks?.firstOrNull()?.removeSink(binding.webRtcView)
                    videoTrack = null
                }
                override fun onDataChannel(channel: DataChannel?) = Unit
                override fun onRenegotiationNeeded() {
                    Timber.d("debug_webrtc onRenegotiationNeeded")
                }
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    Timber.d("debug_webrtc onAddTrack: ${receiver?.track()?.kind()}")
                    val track = receiver?.track() as? org.webrtc.VideoTrack
                    if (track != null) {
                        videoTrack = track
                        track.addSink(binding.webRtcView)
                    }
                }
            }
        )
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiverInit(RtpTransceiverDirection.RECV_ONLY)
        )
        Timber.d("debug_webrtc peer connection created")
        createWebRtcOffer()
        webRtcStarted = true
    }

    private fun createWebRtcOffer() {
        isOfferSent = false
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        Timber.d("debug_webrtc creating offer")
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Timber.d("debug_webrtc offer created")
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Timber.d("debug_webrtc local description set, waiting for ICE gathering")
                        view?.postDelayed({
                            checkAndSendOffer()
                        }, 2000)
                    }
                }, desc)
            }
        }, constraints)
    }

    private fun checkAndSendOffer() {
        if (isOfferSent) return
        val localDescription = peerConnection?.localDescription
        val url = currentWhepUrl
        if (localDescription != null && url != null) {
            isOfferSent = true
            sendWhepOffer(url, localDescription)
        }
    }

    private fun sendWhepOffer(whepUrl: String, desc: SessionDescription) {
        Timber.d("debug_webrtc sending whep offer after ICE gathering")
        lifecycleScope.launch(Dispatchers.IO) {
            val answer = postWhepOffer(whepUrl, desc.description)
            withContext(Dispatchers.Main) {
                when (answer) {
                    "H265_UNSUPPORTED" -> {
                        Toast.makeText(
                            requireContext(),
                            "Не поддерживается формат h265. Переход на HLS.",
                            Toast.LENGTH_SHORT
                        ).show()
                        startHls(args.entranceCamera.hlsUrl.orEmpty())
                    }

                    "H264_UNSUPPORTED" -> {
                        Toast.makeText(
                            requireContext(),
                            "H264 не поддерживается софтварно в WebRTC на этом устройстве. Переход на HLS.",
                            Toast.LENGTH_SHORT
                        ).show()
                        startHls(args.entranceCamera.hlsUrl.orEmpty())
                    }

                    null, "" -> {
                        Timber.d("debug_webrtc whep answer empty, falling back to HLS")
                        startHls(args.entranceCamera.hlsUrl.orEmpty())
                    }

                    else -> {
                        Timber.d("debug_webrtc setting remote description")
                        peerConnection?.setRemoteDescription(
                            object : SdpObserverAdapter() {
                                override fun onSetSuccess() {
                                    Timber.d("debug_webrtc remote description set")
                                }

                                override fun onSetFailure(error: String?) {
                                    Timber.e("debug_webrtc setRemoteDescription failure: $error")
                                    startHls(args.entranceCamera.hlsUrl.orEmpty())
                                }
                            },
                            SessionDescription(SessionDescription.Type.ANSWER, answer)
                        )
                    }
                }
            }
        }
    }

    private fun postWhepOffer(whepUrl: String, offer: String): String? {
        val body = RequestBody.create("application/sdp".toMediaTypeOrNull(), offer)
        val request = Request.Builder()
            .url(whepUrl)
            .method("POST", body)
            .build()
        val client = OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                Timber.d("debug_webrtc postWhepOffer response code: ${response.code}")
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Timber.d("debug_webrtc postWhepOffer response body length: ${responseBody?.length}")
                    responseBody
                } else {
                    val errorBody = response.body?.string()
                    Timber.e("debug_webrtc postWhepOffer error: $errorBody")
                    when {
                        errorBody?.contains("webrtc_offer_missing_codec:h265") == true -> "H265_UNSUPPORTED"
                        errorBody?.contains("webrtc_offer_missing_codec:h264") == true -> "H264_UNSUPPORTED"
                        else -> null
                    }
                }
            }
        }.onFailure { Timber.e("debug_webrtc entrance whep failed $it") }.getOrNull()
    }

    private fun startHls(hlsUrl: String) {
        stopWebRtc()
        if (hlsUrl.isBlank()) {
            binding.progressBar.isVisible = false
            showNoStreamPlaceholder("HLS недоступен по причине: пустой URL")
            return
        }

        binding.playerView.alpha = 1.0f
        hlsPlayer = createHlsPlayer(binding.playerView, binding.progressBar).also { player ->
            player.prepareMedia(hlsUrl, doPlay = true)
        }
    }

    private fun showNoStreamPlaceholder(reason: String) {
        Toast.makeText(requireContext(), reason, Toast.LENGTH_LONG).show()
        binding.ivNoStream.isVisible = true
    }

    private fun createHlsPlayer(videoView: PlayerView, progressView: View): BaseCCTVPlayer {
        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady() {
                progressView.isVisible = false
                binding.ivPreview.isVisible = false
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                (hlsPlayer as? DefaultCCTVPlayer)?.getPlayer()?.videoFormat?.let { format ->
                    if (format.width > 0 && format.height > 0) {
                        binding.zlEntranceCamera.setAspectRatio(format.width.toFloat() / format.height.toFloat())
                    }
                }
            }

            override fun onPlayerStateBuffering() {
                progressView.isVisible = true
            }

            override fun onPlayerStateEnded() {
                progressView.isVisible = false
            }

            override fun onPlayerStateIdle() {
                progressView.isVisible = false
            }

            override fun onPlayerError(exception: Exception) {
                progressView.isVisible = false
                val reason = (exception as? ExoPlaybackException)?.let { "HLS error ${it.type}" } ?: exception.message ?: "Unknown error"
                showNoStreamPlaceholder("HLS недоступен по причине: $reason")
                (exception as? ExoPlaybackException)?.let { Timber.d("Entrance HLS error ${it.type}") }
            }
        }
        return DefaultCCTVPlayer(requireContext(), true, callbacks).also { player ->
            videoView.player = (player as DefaultCCTVPlayer).getPlayer()
            videoView.useController = false
            player.playWhenReady = true
        }
    }

    private fun fitWebRtcView(videoWidth: Int, videoHeight: Int) {
        val w = binding.videoSurfaceWrap.width
        val h = binding.videoSurfaceWrap.height
        if (w > 0 && h > 0 && videoWidth > 0 && videoHeight > 0) {
            val scale = min(h.toFloat() / videoHeight.toFloat(), w.toFloat() / videoWidth.toFloat())
            binding.webRtcView.layoutParams?.let { lp ->
                lp.width = (scale * videoWidth).toInt()
                lp.height = (scale * videoHeight).toInt()
                binding.webRtcView.layoutParams = lp
            }
            binding.zlEntranceCamera.setAspectRatio(videoWidth.toFloat() / videoHeight.toFloat())
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun enterFullscreen() {
        (activity as? MainActivity)?.hideSystemUI()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (activity as? MainActivity)?.showSystemUI()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        releasePlayback()
    }

    override fun onDestroyView() {
        releasePlayback()
        exitFullscreen()
        _binding = null
        super.onDestroyView()
    }

    private fun releasePlayback() {
        hlsPlayer?.releasePlayer()
        hlsPlayer = null
        stopWebRtc()
    }

    private fun stopWebRtc() {
        Timber.d("debug_webrtc stopWebRtc")
        webRtcStarted = false

        binding.webRtcView.alpha = 0.0f

        videoTrack?.removeSink(binding.webRtcView)
        videoTrack = null

        runCatching { binding.webRtcView.release() }

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) {
            Timber.e("debug_webrtc onCreateFailure: $error")
        }
        override fun onSetFailure(error: String?) {
            Timber.e("debug_webrtc onSetFailure: $error")
        }
    }
}
