package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import com.sesameware.data.repository.WebRtcMediaTrack
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.smartyard_oem.databinding.FragmentEntranceCameraBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
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
        bindViews()
        collectWebRtcState()
    }

    private fun collectWebRtcState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.webRtcState.transformWhile { state ->
                    emit(state)
                    state !is WebRtcState.Error
                }.collect { state ->
                    when (state) {
                        is WebRtcState.Connected -> {
                            val (video, audio) = state.unpackToPair()
                            Timber.d("debug_webrtc starting playthrough: ${video?.javaClass?.simpleName} ${audio?.javaClass?.simpleName}}")
                            videoTrack = video
                            audioTrack = audio
                            startWebRtc()
                            mode = Mode.WEB_RTC
                        }
                        is WebRtcState.Disconnected -> releaseWebRtcView()
                        is WebRtcState.Error -> {
                            Timber.d("debug_webrtc connection failed: $state")
                            mode = Mode.HLS
                            releaseWebRtcView()
                            createHlsPlayer(binding.playerView, binding.progressBar)
                            startHls()
                        }
                        else -> {}
                    }
                }
            }
        }
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

        videoTrack = null
        started = false
        runCatching { binding.webRtcView.release() }
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

        binding.progressBar.isVisible = true
        binding.webRtcView.run {
            setEnableHardwareScaler(true)

            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            init(rootEglBase.eglBaseContext, object : RendererEvents {
                override fun onFirstFrameRendered() {
                    post {
                        Timber.d("debug_webrtc onFirstFrameRendered")
                        binding.progressBar.isVisible = false
                        binding.ivPreview.isVisible = false
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

    private fun showNoStreamPlaceholder(reason: String) {
        Toast.makeText(requireContext(), reason, Toast.LENGTH_LONG).show()
        binding.ivNoStream.isVisible = true
    }

    private fun createHlsPlayer(videoView: PlayerView, progressView: View) {
        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady() {
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
                val reason = (exception as? ExoPlaybackException)?.let { "HLS error ${it.type}" } ?: exception.message ?: "Unknown error"
                showNoStreamPlaceholder("HLS недоступен по причине: $reason")
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

    private fun startHls() {
        val hlsUrl = args.entranceCamera.hlsUrl
        if (hlsUrl.isBlank()) {
            binding.progressBar.isVisible = false
            showNoStreamPlaceholder("HLS недоступен по причине: пустой URL")
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
        (activity as? MainActivity)?.hideSystemUI(false)
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
