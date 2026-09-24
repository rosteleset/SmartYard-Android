package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import com.sesameware.data.repository.WebRtcMediaTrack
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.databinding.FragmentEntranceCameraBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoTrack
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class EntranceCameraFragment : Fragment() {
    private var _binding: FragmentEntranceCameraBinding? = null
    private val binding get() = _binding!!

    private val args: EntranceCameraFragmentArgs by navArgs()
    private val viewModel by sharedViewModel<AddressViewModel>()

    private var mode = Mode.IDLE
    private var started = false
    private val rootEglBase: EglBase by inject()
    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null
    private var hlsPlayer: BaseCCTVPlayer? = null
    private var firstFrameTimeoutJob: Job? = null
    private var webRtcCollectionJob: Job? = null

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
        binding.connectionLog.isVisible = BuildConfig.DEBUG
        enterFullscreen()
        bindViews()
        collectWebRtcState()
    }

    private fun collectWebRtcState() {
        webRtcCollectionJob?.cancel()
        webRtcCollectionJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.webRtcState.transformWhile { state ->
                    emit(state)
                    state !is WebRtcState.Error
                }.collect { state ->
                    when (state) {
                        is WebRtcState.Connected -> {
                            val (video, audio) = state.unpackToPair()
                            val noVideo = if (video == null) " not" else ""
                            val noAudio = if (audio == null) " not" else ""
                            addDebugConnectionState("Starting WebRTC: video$noVideo connected, audio$noAudio connected",)
                            addDebugConnectionState("Playing ${args.entranceCamera.whepUrl}")
                            Timber.d("debug_webrtc starting playthrough: ${video?.javaClass?.simpleName} ${audio?.javaClass?.simpleName}")
                            videoTrack = video
                            audioTrack = audio
                            startWebRtc()
                            mode = Mode.WEB_RTC

                            firstFrameTimeoutJob?.cancel()
                            firstFrameTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
                                delay(2000.milliseconds)
                                val timeoutMsg = "WebRTC first frame waiting timeout"
                                addDebugConnectionState("WebRTC connection error: $timeoutMsg")
                                Timber.d("debug_webrtc connection error:  $timeoutMsg")
                                fallbackToHls()
                            }
                        }
                        is WebRtcState.Disconnected -> releaseWebRtcView()
                        is WebRtcState.Error -> {
                            addDebugConnectionState("WebRTC connection error: ${state.message}")
                            Timber.d("debug_webrtc connection error:  ${state.message},\ncause:  ${state.cause}")
                            fallbackToHls()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun fallbackToHls() {
        webRtcCollectionJob?.cancel()
        addDebugConnectionState("Fallback to HLS")
        mode = Mode.HLS
        releaseWebRtcView()
        createHlsPlayer(binding.playerView, binding.progressBar)
        startHls()
    }

    private fun startWebRtc() {
        videoTrack?.apply {
            setEnabled(true)
            addSink(binding.webRtcView)
        }

        audioTrack?.let { audio ->
            with (binding) {
                ivMute.isSelected = true
                audio.setEnabled(false)

                ivMute.setOnClickListener {
                    audio.setEnabled(!audio.enabled())
                    ivMute.isSelected = !audio.enabled()
                }

                muteWrapper.isVisible = true
            }
        }

        started = true
    }

    private fun stopWebRtc() {
        Timber.d("debug_webrtc stopWebRtc")
        firstFrameTimeoutJob?.cancel()

        videoTrack?.apply {
            removeSink(binding.webRtcView)
            setEnabled(false)
        }

        audioTrack?.let { audio ->
            with (binding) {
                ivMute.isSelected = true
                audio.setEnabled(false)
            }
        }

        started = false
    }

    private fun releaseWebRtcView() {
        Timber.d("debug_webrtc releaseWebRtc")
        firstFrameTimeoutJob?.cancel()

        videoTrack = null
        started = false
        runCatching { binding.webRtcView.release() }
    }

    private fun bindViews() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tbOpen.setOnClickListener {
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
        binding.webRtcView.run {
            setEnableHardwareScaler(true)

            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            init(rootEglBase.eglBaseContext, object : RendererEvents {
                override fun onFirstFrameRendered() {
                    firstFrameTimeoutJob?.cancel()
                    post {
                        Timber.d("debug_webrtc onFirstFrameRendered")
                        binding.progressBar.isVisible = false
                        binding.ivPreview.isVisible = false
                        binding.ivNoStream.isVisible = false
                        alpha = 1.0f
                    }
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    post {
                        Timber.d("debug_webrtc FrameResolutionChanged")
                        val isPortrait = rotation == 90 || rotation == 270
                        val actualWidth = if (isPortrait) videoHeight else videoWidth
                        val actualHeight = if (isPortrait) videoWidth else videoHeight

                        if (actualWidth > 0 && actualHeight > 0) {
                            val ratio = actualWidth.toFloat() / actualHeight.toFloat()
                            binding.zlEntranceCamera.setAspectRatio(ratio)
                        }
                    }
                }
            })
        }
    }

    private fun addDebugConnectionState(newLine: String, error: Boolean = false) {
        val prevText = binding.connectionLog.text
        val builder = SpannableStringBuilder(prevText)
        if (prevText.isNotEmpty()) {
            builder.append("\n")
        }
        val start = builder.length
        builder.append(newLine)
        if (error) {
            builder.setSpan(
                ForegroundColorSpan(Color.RED),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.connectionLog.text = builder
    }

    private fun createHlsPlayer(videoView: PlayerView, progressView: View) {
        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady(player: DefaultCCTVPlayer) {
                addDebugConnectionState("HLS player ready",)
                val hasLL = if (player.isLlHlsStream()) "LL-" else ""
                addDebugConnectionState("Playing ${hasLL}HLS stream ${args.entranceCamera.hlsUrl}",)
                binding.ivNoStream.isVisible = false
                Timber.d("debug_dmm hls onPlayerStateReady")
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
                binding.ivNoStream.isVisible = true
                val reason = resolveError(exception)
                addDebugConnectionState("HLS play error: $reason", error = true)
                mode = Mode.STOPPED
                (exception as? ExoPlaybackException)?.let { Timber.d("Entrance HLS error ${it.type}") }
            }
        }

        hlsPlayer = DefaultCCTVPlayer(requireContext(), true, callbacks).also { player ->
            videoView.player = (player as DefaultCCTVPlayer).getPlayer()
            videoView.useController = false
            player.playWhenReady = true
        }
    }

    private fun resolveError(exception: Exception): String =
        (exception as? ExoPlaybackException)?.let {
            "HLS error ${it.message}"
        } ?: exception.message
        ?: "Unknown error"

    private fun startHls() {
        val hlsUrl = args.entranceCamera.hlsUrl
        if (hlsUrl.isBlank()) {
            binding.progressBar.isVisible = false
            addDebugConnectionState("HLS play error: empty URL", error = true)
            binding.ivNoStream.isVisible = true
            return
        }

        hlsPlayer?.prepareMedia(hlsUrl, doPlay = true)

        binding.playerView.alpha = 1.0f

        started = true
    }

    private fun stopHls() {
        hlsPlayer?.stop()

        started = false
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

        if (!started) return

        when (mode) {
            Mode.WEB_RTC -> stopWebRtc()
            Mode.HLS -> stopHls()
            else -> {}
        }
    }

    override fun onStart() {
        super.onStart()

        if (started) return

        when (mode) {
            Mode.WEB_RTC -> startWebRtc()
            Mode.HLS -> startHls()
            else -> {}
        }
    }

    override fun onDestroyView() {
        releaseWebRtcView()
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

    private enum class Mode {
        IDLE,
        WEB_RTC,
        HLS,
        STOPPED
    }
}
