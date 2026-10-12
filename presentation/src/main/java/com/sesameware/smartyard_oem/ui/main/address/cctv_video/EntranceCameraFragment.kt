package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.exoplayer2.PlaybackException
import com.sesameware.data.player.log.PlayerLogger
import com.sesameware.data.repository.WebRtcMediaTrack
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.model.NetworkStatus
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentEntranceCameraBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.common.player.BaseCCTVPlayer
import com.sesameware.smartyard_oem.ui.common.player.DefaultCCTVPlayer
import com.sesameware.smartyard_oem.ui.common.player.FallbackMediaSourceFactory
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import com.sesameware.smartyard_oem.ui.main.player.log.PlayerLogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoTrack
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class EntranceCameraFragment : Fragment() {
    companion object {
        private const val WEB_RTC_FF_TIMEOUT = 8000
        private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f
    }
    private var _binding: FragmentEntranceCameraBinding? = null
    private val binding get() = _binding!!

    private val args: EntranceCameraFragmentArgs by navArgs()
    private val viewModel by sharedViewModel<AddressViewModel>()

    private var isWebRtcFailed = false
    private var isWebRtcTracksAttached = false
    private var isWebRtcRendererInitialized = false
    private val rootEglBase: EglBase by inject()
    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null
    private var hlsPlayer: DefaultCCTVPlayer? = null
    private var webRtcFirstFrameTimeoutJob: Job? = null
    private var playerNetworkJob: Job? = null
    private var webRtcFrameListener: EglRenderer.FrameListener? = null
    private var webRtcFirstFrameRendered = false
    private var webRtcAttachmentId = 0L
    private var webRtcAttachedAtMs = 0L
    private val mediaSourceFactory = FallbackMediaSourceFactory()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntranceCameraBinding.inflate(inflater, container, false)
        binding.root.applyBottomNavInsetsToPadding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        enterFullscreen()
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        bindViews()
        createHlsPlayer()
        runWebRtc()
    }

    private fun bindViews() {
        // Only the active video surface is visible. Preview masks it until a frame
        // reaches the renderer; SurfaceView alpha is not a reliable surface switch.
        binding.playerView.isVisible = false
        binding.webRtcView.isVisible = true
        binding.webRtcView.alpha = 1f
        binding.ivPreview.isVisible = true

        if (BuildConfig.DEBUG){
            binding.ivPlayerLog.isVisible = true

            val fragment = PlayerLogFragment()
            val tag = PlayerLogFragment.TAG
            childFragmentManager.beginTransaction()
                .add(R.id.playerLogContainer, fragment, tag)
                .hide(fragment)
                .commit()

            binding.ivPlayerLog.setOnClickListener {
                childFragmentManager.beginTransaction()
                    .show(fragment)
                    .commit()
            }
        }
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tbOpen.setOnClickListener {
            Timber.d(
                "%s%s",
                "debug_dmm Opening door: domophoneId=${args.lock.domophoneId}, ",
                "doorId=${args.lock.doorId}"
            )
            viewModel.openDoor(args.lock)
            _binding?.tbOpen?.isClickable = false
            _binding?.root?.postDelayed(
                {
                    _binding?.tbOpen?.isChecked = false
                    _binding?.tbOpen?.isClickable = true
                },
                3000
            )
        }

        Glide.with(binding.ivPreview)
            .load(args.entranceCamera.previewUrl)
            .signature(ObjectKey(args.entranceCamera.previewCacheKey))
            .into(binding.ivPreview)

        binding.progressBar.isVisible = true
        binding.zlEntranceCamera.setAspectRatio(DEFAULT_VIDEO_ASPECT_RATIO)
        binding.webRtcView.run {
            setEnableHardwareScaler(true)

            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            init(rootEglBase.eglBaseContext, object : RendererEvents {
                override fun onFirstFrameRendered() {
                    // SurfaceEglRenderer reports an incoming frame BEFORE its GL draw,
                    // once per init(), not once per attachment. Diagnostic only.
                    PlayerLogger.d("[WRTC-MEDIA] Renderer received its first frame since init (not render-ready)")
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    val renderer = this@run
                    renderer.post {
                        val currentBinding = _binding ?: return@post
                        if (currentBinding.webRtcView !== renderer || isWebRtcFailed || !isWebRtcTracksAttached) return@post
                        PlayerLogger.i("[WRTC-MEDIA] Video resolution:" +
                                " ${videoWidth}x${videoHeight}, rotation: $rotation")
                        val isPortrait = rotation == 90 || rotation == 270
                        val actualWidth = if (isPortrait) videoHeight else videoWidth
                        val actualHeight = if (isPortrait) videoWidth else videoHeight

                        if (actualWidth > 0 && actualHeight > 0) {
                            currentBinding.zlEntranceCamera.setAspectRatio(actualWidth.toFloat() / actualHeight.toFloat())
                        }
                    }
                }
            })
            isWebRtcRendererInitialized = true
        }
    }

    private fun runWebRtc() {
        val whepUrl = args.entranceCamera.whepUrl
        if (whepUrl.isBlank()) {
            PlayerLogger.w("[FLOW] Fallback to LL-HLS (Reason: WHEP URL is blank)")
            isWebRtcFailed = true
            runHls()
            return
        }

        playerNetworkJob?.cancel()
        playerNetworkJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    if (isWebRtcFailed) {
                        runHls()
                        return@repeatOnLifecycle
                    }

                    viewModel.networkStatus.collectLatest { status ->
                        when (status) {
                            is NetworkStatus.Disconnected -> {
                                PlayerLogger.w("[NETWORK] Network lost. Detaching WebRTC tracks...")
                                binding.progressBar.isVisible = true
                                detachWebRtcTracks()
                            }

                            is NetworkStatus.AvailableWiFi,
                            is NetworkStatus.AvailableCellular -> {
                                while (isActive && !isWebRtcFailed) {
                                    PlayerLogger.d("[NETWORK] Network available. Connecting WebRTC...")
                                    startWebRtcFlow(whepUrl)
                                    if (isWebRtcFailed) break
                                    PlayerLogger.w("[NETWORK] WebRTC stream disconnected or error." +
                                            " Retrying in 2 seconds...")
                                    delay(2000.milliseconds)
                                }
                            }

                            is NetworkStatus.Undefined -> {
                                PlayerLogger.d("[NETWORK] Network Undefined...")
                            }
                        }

                    }
                } finally {
                    // A fallback cancels this collector. Its cleanup must not reset
                    // the HLS UI/audio controls after runHls() has taken ownership.
                    if (!isWebRtcFailed) detachWebRtcTracks()
                }
            }
        }
    }

    private suspend fun startWebRtcFlow(whepUrl: String) {
        viewModel.playWebRtcStream(whepUrl)
            .transformWhile { state ->
                emit(state)
                state !is WebRtcState.Fatal
            }
            .collect { state ->
                when (state) {
                    is WebRtcState.Connected -> {
                        binding.progressBar.isVisible = true
                        PlayerLogger.d("[WRTC-MEDIA] Connected delivered at elapsedRealtimeMs=${SystemClock.elapsedRealtime()}; waiting for rendered video")
                        val (video, audio) = state.unpackToPair()
                        attachWebRtcTracks(video, audio)
                    }

                    is WebRtcState.Fatal -> {
                        PlayerLogger.w("[FLOW] Fatal WebRTC Error. Fallback to LL-HLS")
                        detachWebRtcTracks()
                        isWebRtcFailed = true
                        runHls()
                    }

                    is WebRtcState.Disconnected,
                    is WebRtcState.Error -> {
                        binding.progressBar.isVisible = true
                        detachWebRtcTracks()
                    }

                    else -> {}
                }
            }
    }

    private fun attachWebRtcTracks(video: VideoTrack?, audio: AudioTrack?) {
        PlayerLogger.i("[FLOW] attachWebRtcTracks called")
        if (isWebRtcFailed || !isWebRtcRendererInitialized) return
        if (isWebRtcTracksAttached) {
            PlayerLogger.w("[FLOW] Tracks already attached!")
            return
        }
        val renderer = binding.webRtcView
        val attachmentId = ++webRtcAttachmentId
        val attachedAtMs = SystemClock.elapsedRealtime()
        webRtcAttachedAtMs = attachedAtMs
        videoTrack = video
        audioTrack = audio
        webRtcFirstFrameRendered = false
        isWebRtcTracksAttached = true
        binding.playerView.isVisible = false
        renderer.isVisible = true
        renderer.alpha = 1f
        binding.ivPreview.isVisible = true
        binding.progressBar.isVisible = true
        binding.ivNoStream.isVisible = false
        // FrameListener's exposed overload ignores FPS reduction. Explicitly use
        // unrestricted rendering so its notification follows the normal GL draw.
        renderer.disableFpsReduction()

        webRtcFrameListener?.let { listener ->
            runCatching { renderer.removeFrameListener(listener) }
        }
        val listener = object : EglRenderer.FrameListener {
            override fun onFrame(bitmap: Bitmap?) {
                val renderedAtMs = SystemClock.elapsedRealtime()
                renderer.post {
                    val currentBinding = _binding ?: return@post
                    if (currentBinding.webRtcView !== renderer ||
                        attachmentId != webRtcAttachmentId ||
                        !isWebRtcTracksAttached || isWebRtcFailed || webRtcFirstFrameRendered
                    ) return@post
                    // EglRenderer removes FrameListeners before calling them.
                    // scale=0 deliberately delivers null without GPU readback.
                    webRtcFrameListener = null
                    webRtcFirstFrameTimeoutJob?.cancel()
                    PlayerLogger.d("[WRTC-MEDIA] First frame rendered on webRtcView" +
                            " attachment=$attachmentId elapsedRealtimeMs=$renderedAtMs" +
                            " attachToRenderMs=${renderedAtMs - attachedAtMs}" +
                            " uiDispatchMs=${SystemClock.elapsedRealtime() - renderedAtMs}")
                    webRtcFirstFrameRendered = true
                    showWebRtcWhenReady()
                }
            }
        }
        webRtcFrameListener = listener
        // Register BEFORE attaching the sink so an immediate frame is not missed.
        renderer.addFrameListener(listener, 0f)
        runCatching { videoTrack?.setEnabled(true) }
            .onFailure { PlayerLogger.e(it, "[WRTC-MEDIA] Failed to enable video track") }
        runCatching { videoTrack?.addSink(renderer) }
            .onFailure { PlayerLogger.e(it, "[WRTC-MEDIA] Failed to attach video sink") }

        with(binding) {
            val soundEnabled = args.entranceCamera.enableSound
            muteWrapper.isVisible = soundEnabled && audioTrack != null
            ivMute.isSelected = true
            runCatching { audioTrack?.setEnabled(false) }

            ivMute.setOnClickListener {
                val isAudioEnabled = ivMute.isSelected
                runCatching {
                    val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    if (audioManager?.mode != AudioManager.MODE_NORMAL) {
                        audioManager?.mode = AudioManager.MODE_NORMAL
                    }
                }
                runCatching { audioTrack?.setEnabled(isAudioEnabled) }
                ivMute.isSelected = !isAudioEnabled
                logAudioState(soundEnabled)
            }
            logAudioState(soundEnabled)
        }

        webRtcFirstFrameTimeoutJob?.cancel()
        webRtcFirstFrameTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            val timeout = WEB_RTC_FF_TIMEOUT
            PlayerLogger.i("[WRTC-MEDIA] Waiting for first frame (Timeout: ${timeout}ms)")
            delay(timeout.milliseconds)
            if (attachmentId != webRtcAttachmentId || webRtcFirstFrameRendered) return@launch
            PlayerLogger.w("[FLOW] Fallback to LL-HLS (Reason: first frame waiting timeout;" +
                    " attachment=$attachmentId waitedMs=${SystemClock.elapsedRealtime() - webRtcAttachedAtMs})")
            detachWebRtcTracks()
            isWebRtcFailed = true
            runHls()
        }
    }

    private fun logAudioState(soundEnabled: Boolean) {
        val actualAudioEnabled = runCatching { audioTrack?.enabled() == true }.getOrDefault(false)
        val rbtSoundState = if (soundEnabled) "enabled" else "disabled"
        val trackEnabled = if (actualAudioEnabled) "enabled" else "disabled"
        val audioTrackState = if (soundEnabled) "; audio track is $trackEnabled" else ""
        val message = "[WRTC-MEDIA] Sound is $rbtSoundState by RBT$audioTrackState"
        if (soundEnabled && actualAudioEnabled) {
            PlayerLogger.d(message)
        } else {
            PlayerLogger.w(message)
        }
    }

    private fun showWebRtcWhenReady() {
        val currentBinding = _binding ?: return
        if (!webRtcFirstFrameRendered || !isWebRtcTracksAttached || isWebRtcFailed) return
        // View pre-draw does not synchronize with SurfaceView's GL rendering.
        currentBinding.ivPreview.isVisible = false
        currentBinding.progressBar.isVisible = false
        currentBinding.ivNoStream.isVisible = false
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun detachWebRtcTracks() {
        PlayerLogger.i("[FLOW] detachWebRtcTracks called")
        ++webRtcAttachmentId // Invalidate callbacks already posted to the UI thread.
        webRtcFirstFrameTimeoutJob?.cancel()
        val currentBinding = _binding ?: return

        webRtcFrameListener?.let { listener ->
            runCatching { currentBinding.webRtcView.removeFrameListener(listener) }
            webRtcFrameListener = null
        }

        val vTrack = videoTrack
        val aTrack = audioTrack

        videoTrack = null
        audioTrack = null
        isWebRtcTracksAttached = false
        webRtcFirstFrameRendered = false

        if (vTrack != null) {
            runCatching { vTrack.removeSink(binding.webRtcView) }
            runCatching { vTrack.setEnabled(false) }
        }

        currentBinding.ivPreview.isVisible = true
        currentBinding.progressBar.isVisible = true
        if (isWebRtcRendererInitialized) {
            runCatching { currentBinding.webRtcView.clearImage() }
        }

        if (aTrack != null) {
            runCatching { aTrack.setEnabled(false) }
        }

        with(binding) {
            ivMute.isSelected = true
            ivMute.setOnClickListener(null)
            muteWrapper.isVisible = false
        }
    }

    @MainThread
    private fun releaseWebRtc() {
        if (!isWebRtcRendererInitialized) return
        val renderer = _binding?.webRtcView ?: return
        if (isWebRtcTracksAttached || webRtcFrameListener != null) {
            detachWebRtcTracks()
        }
        // Invalidate queued frame callbacks before stopping the EGL thread.
        ++webRtcAttachmentId
        webRtcFirstFrameTimeoutJob?.cancel()
        isWebRtcRendererInitialized = false
        PlayerLogger.i("[FLOW] Releasing WebRTC EGL renderer")
        runCatching { renderer.release() }
            .onFailure { PlayerLogger.e(it, "[FLOW] Failed to release WebRTC EGL renderer") }
    }

    private fun createHlsPlayer(): DefaultCCTVPlayer {
        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady(player: DefaultCCTVPlayer) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                hlsPlayer?.getPlayer()?.videoFormat?.let { format ->
                    PlayerLogger.d("[HLS] video format: ${format.width}x${format.height}, ${format.frameRate.toInt()} fps")
                    if (format.width > 0 && format.height > 0) {
                        binding.zlEntranceCamera.setAspectRatio(format.width.toFloat() / format.height.toFloat())
                    }
                }

            }

            override fun onRenderFirstFrame() {
                val currentBinding = _binding ?: return
                if (!isWebRtcFailed || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
                PlayerLogger.d("[HLS-MEDIA] First frame rendered; revealing HLS surface")
                currentBinding.playerView.alpha = 1f
                currentBinding.ivPreview.isVisible = false
                currentBinding.progressBar.isVisible = false
                currentBinding.ivNoStream.isVisible = false
            }

            override fun onAudioAvailabilityChanged(isAvailable: Boolean) {
                if (isWebRtcFailed) {
                    binding.muteWrapper.isVisible = isAvailable
                    if (isAvailable) {
                        hlsPlayer?.isMuted = true
                        binding.ivMute.isSelected = true
                        binding.ivMute.setOnClickListener {
                            val mute = hlsPlayer?.isMuted != true
                            hlsPlayer?.isMuted = mute
                            binding.ivMute.isSelected = mute
                            PlayerLogger.d("[HLS-MEDIA] Audio ${if (!mute) "enabled" else "disabled"}")
                        }
                    }
                }
            }

            override fun onPlayerError(exception: Exception) {
                val playbackException = exception as? PlaybackException
                if (playbackException?.errorCode ==
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    playbackException?.errorCode ==
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                ) {
                    PlayerLogger.w("[HLS] Network timeout caught! Stopping and waiting for network...")
                    binding.progressBar.isVisible = true
                    hlsPlayer?.stop()
                }
                PlayerLogger.e(message = "[HLS] Fatal fail")
                binding.progressBar.isVisible = false
                binding.ivNoStream.isVisible = true
            }
        }

        hlsPlayer = DefaultCCTVPlayer(requireContext(), callbacks)
        return hlsPlayer!!
    }

    @MainThread
    private fun runHls() {
        playerNetworkJob?.cancel()
        // Hide/destroy the WebRTC surface, not just its View alpha. Two visible
        // SurfaceViews in the same container can obscure the fallback's video.
        binding.webRtcView.isVisible = false
        // HLS fallback is final for this view; no need to keep an idle EGL thread.
        releaseWebRtc()
        binding.playerView.isVisible = true
        binding.playerView.alpha = 1f
        binding.ivPreview.isVisible = true
        binding.progressBar.isVisible = true
        binding.ivNoStream.isVisible = false
        val llHlsUrl = args.entranceCamera.llHlsUrl
        val hlsUrl = args.entranceCamera.hlsUrl
        if (llHlsUrl.isBlank() || hlsUrl.isBlank()) {
            val ll = if (llHlsUrl.isBlank()) "LL-HLS URL is blank" else ""
            val hls = if (hlsUrl.isBlank()) ", HLS URL is blank" else ""
            PlayerLogger.e(message = "[(LL)HLS] Preparation error: (Reason: $ll$hls)")
            binding.progressBar.isVisible = false
            binding.ivNoStream.isVisible = true
            return
        }

        val mediaSource = mediaSourceFactory.createSource(llHlsUrl, hlsUrl)
        hlsPlayer?.setMediaSource(mediaSource, enableSound = args.entranceCamera.enableSound)
        playerNetworkJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.networkStatus.collectLatest { status ->
                    when (status) {
                        is NetworkStatus.Disconnected -> {
                            PlayerLogger.w("[NETWORK] Network lost. Stopping HLS to prevent timeout...")
                            binding.progressBar.isVisible = true
                            hlsPlayer?.stop()
                        }

                        is NetworkStatus.AvailableWiFi,
                        is NetworkStatus.AvailableCellular -> {
                            PlayerLogger.d("[NETWORK] Network available. Resuming HLS from Live edge...")
                            hlsPlayer?.let { player ->
                                binding.ivPreview.isVisible = true
                                binding.progressBar.isVisible = true
                                binding.ivNoStream.isVisible = false
                                if (binding.playerView.player == null) {
                                    PlayerLogger.d("[(LL)HLS] Assigning Player before prepare")
                                    binding.playerView.player = player.getPlayer()
                                }
                                player.seekToDefaultPosition()
                                player.prepare()
                                player.play()
                            }
                        }

                        is NetworkStatus.Undefined -> {
                            PlayerLogger.d("[NETWORK] Network Undefined...")
                        }
                    }
                }
            }
        }
    }

    private fun releaseHls() {
        hlsPlayer?.releasePlayer()
        hlsPlayer = null
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

        if (isWebRtcFailed) {
            hlsPlayer?.stop()
        } else {
            detachWebRtcTracks()
        }

        binding.ivPreview.isVisible = true
        binding.progressBar.isVisible = true
    }

    override fun onDestroyView() {
        playerNetworkJob?.cancel()
        webRtcFirstFrameTimeoutJob?.cancel()
        detachWebRtcTracks()
        releaseWebRtc()
        releaseHls()
        exitFullscreen()
        _binding = null

        super.onDestroyView()
    }

    private fun WebRtcState.Connected.unpackToPair(): Pair<VideoTrack?, AudioTrack?> {
        val video = (this.videoTrack as? WebRtcMediaTrack)?.rtcTrack as? VideoTrack
        val audio = (this.audioTrack as? WebRtcMediaTrack)?.rtcTrack as? AudioTrack

        return video to audio
    }
}
