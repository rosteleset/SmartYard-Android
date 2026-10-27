package com.sesameware.smartyard_oem.ui.common.player

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.SystemClock
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.exoplayer2.PlaybackException
import com.sesameware.data.player.log.PlayerLogger
import com.sesameware.data.repository.WebRtcMediaTrack
import com.sesameware.domain.interfaces.NetworkMonitor
import com.sesameware.domain.interfaces.PlayerLogRepository
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.interactors.WebRtcStreamingInteractor
import com.sesameware.domain.model.NetworkStatus
import com.sesameware.domain.model.response.MediaServerType
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.ui.main.player.log.PlayerLogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import kotlin.time.Duration.Companion.milliseconds

class CCTVStreamPlayerManager(
    private val context: Context,
    private val rootEglBase: EglBase,
    private val webRtcInteractor: WebRtcStreamingInteractor,
    private val networkMonitor: NetworkMonitor,
    private val playerLogRepository: PlayerLogRepository
) : DefaultLifecycleObserver {

    companion object {
        private const val WEB_RTC_FF_TIMEOUT = 8000L
        private const val DEFAULT_ASPECT_RATIO = 16f / 9f
    }

    private var playerView: CCTVPlayerView? = null
    private var config: StreamSourceConfig? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private var muteButton: View? = null
    private var playerLogButton: View? = null
    private var fragmentManager: FragmentManager? = null
    private var playerLogContainerId: Int? = null

    private var hlsPlayer: BaseCCTVPlayer? = null
    private val mediaSourceFactory = FallbackMediaSourceFactory()

    private var isWebRtcFailed = false
    private var isWebRtcTracksAttached = false
    private var isWebRtcRendererInitialized = false
    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null

    private var playerNetworkJob: Job? = null
    private var webRtcFirstFrameTimeoutJob: Job? = null
    private var webRtcFrameListener: EglRenderer.FrameListener? = null
    private var webRtcFirstFrameRendered = false
    private var webRtcAttachmentId = 0L

    private var isHlsFallbackActive = false
    private var llHlsRetryCount = 0
    private var hlsRetryCount = 0

    var isMuted: Boolean = true
        private set

    private var onMuteChangedListener: ((Boolean) -> Unit)? = null
    private var onErrorListener: ((Exception) -> Unit)? = null

    fun setOnMuteChangedListener(listener: ((Boolean) -> Unit)?) {
        onMuteChangedListener = listener
    }

    fun setOnErrorListener(listener: ((Exception) -> Unit)?) {
        onErrorListener = listener
    }

    fun attach(
        lifecycleOwner: LifecycleOwner,
        playerView: CCTVPlayerView,
        config: StreamSourceConfig,
        muteButton: View? = null,
        playerLogButton: View? = null,
        fragmentManager: FragmentManager? = null,
        playerLogContainerId: Int? = null
    ) {
        detach()
        // Fallback belongs to the current camera, not to the manager's lifetime.
        isWebRtcFailed = false
        isHlsFallbackActive = false
        llHlsRetryCount = 0
        hlsRetryCount = 0
        isMuted = true

        this.lifecycleOwner = lifecycleOwner
        this.playerView = playerView
        this.config = config
        this.muteButton = muteButton
        this.playerLogButton = playerLogButton
        this.fragmentManager = fragmentManager
        this.playerLogContainerId = playerLogContainerId

        lifecycleOwner.lifecycle.addObserver(this)
        setupViews()
        setupPlayerLog()
        createHlsPlayer()
        runWebRtc()
    }

    private fun setupViews() {
        val view = playerView ?: return
        val cfg = config ?: return

        view.setAspectRatio(DEFAULT_ASPECT_RATIO)
        view.loadPreview(cfg.previewUrl, cfg.previewCacheKey)
        view.showWebRtcSurface()
        view.showLoading()

        view.binding.webRtcView.run {
            setEnableHardwareScaler(true)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            init(rootEglBase.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    PlayerLogger.d("[WRTC-MEDIA] Renderer received its first frame since init")
                }

                override fun onFrameResolutionChanged(vw: Int, vh: Int, rotation: Int) {
                    post {
                        val isPortrait = rotation == 90 || rotation == 270
                        val w = if (isPortrait) vh else vw
                        val h = if (isPortrait) vw else vh
                        if (w > 0 && h > 0) {
                            view.setAspectRatio(w.toFloat() / h.toFloat())
                        }
                    }
                }
            })
            isWebRtcRendererInitialized = true
        }
    }

    private fun setupPlayerLog() {
        // Each attachment starts a new camera log, including when the log panel is hidden.
        if (BuildConfig.DEBUG) playerLogRepository.clear()

        val btn = playerLogButton ?: return
        val fm = fragmentManager
        val containerId = playerLogContainerId ?: playerView?.binding?.playerLogContainer?.id

        if (!BuildConfig.DEBUG || fm == null || containerId == null) {
            btn.isVisible = false
            return
        }

        btn.isVisible = true

        val tag = PlayerLogFragment.TAG
        val existing = fm.findFragmentByTag(tag)
        if (existing == null) {
            val fragment = PlayerLogFragment()
            fm.beginTransaction()
                .add(containerId, fragment, tag)
                .hide(fragment)
                .commit()
        }

        btn.setOnClickListener {
            val currentFm = fragmentManager ?: return@setOnClickListener
            val targetContainer = playerLogContainerId ?: playerView?.binding?.playerLogContainer?.id ?: return@setOnClickListener
            val currentTag = PlayerLogFragment.TAG
            val currentFragment = currentFm.findFragmentByTag(currentTag)
            if (currentFragment != null && currentFragment.isAdded) {
                if (currentFragment.isHidden) {
                    currentFm.beginTransaction().show(currentFragment).commit()
                } else {
                    currentFm.beginTransaction().hide(currentFragment).commit()
                }
            } else {
                val fragment = PlayerLogFragment()
                currentFm.beginTransaction()
                    .add(targetContainer, fragment, currentTag)
                    .commit()
            }
        }
    }

    private fun runWebRtc() {
        val cfg = config ?: return
        val owner = lifecycleOwner ?: return

        if (cfg.whepUrl.isBlank()) {
            PlayerLogger.w("[FLOW] Fallback to LL-HLS (Reason: WHEP URL is blank)")
            isWebRtcFailed = true
            runHls()
            return
        }

        playerNetworkJob?.cancel()
        playerNetworkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (isWebRtcFailed) {
                    runHls()
                    return@repeatOnLifecycle
                }

                networkMonitor.networkStatus.collectLatest { status ->
                    when (status) {
                        is NetworkStatus.Disconnected -> {
                            PlayerLogger.w("[NETWORK] Network lost. Detaching WebRTC tracks...")
                            playerView?.showLoading()
                            detachWebRtcTracks()
                        }
                        is NetworkStatus.AvailableWiFi,
                        is NetworkStatus.AvailableCellular -> {
                            while (isActive && !isWebRtcFailed) {
                                PlayerLogger.d("[NETWORK] Network available. Connecting WebRTC...")
                                startWebRtcFlow(cfg.whepUrl)
                                if (isWebRtcFailed) break
                                PlayerLogger.w("[NETWORK] WebRTC stream disconnected or error. Retrying in 2 seconds...")
                                delay(2000.milliseconds)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private suspend fun startWebRtcFlow(whepUrl: String) {
        webRtcInteractor.playStream(whepUrl)
            .transformWhile { state ->
                emit(state)
                state !is WebRtcState.Fatal
            }
            .collect { state ->
                when (state) {
                    is WebRtcState.Connected -> {
                        playerView?.showLoading()
                        val video = (state.videoTrack as? WebRtcMediaTrack)?.rtcTrack as? VideoTrack
                        val audio = (state.audioTrack as? WebRtcMediaTrack)?.rtcTrack as? AudioTrack
                        attachWebRtcTracks(video, audio)
                    }
                    is WebRtcState.Fatal -> {
                        PlayerLogger.w("[FLOW] Fatal WebRTC Error. Fallback to LL-HLS")
                        detachWebRtcTracks()
                        isWebRtcFailed = true
                        runHls()
                    }
                    is WebRtcState.Disconnected, is WebRtcState.Error -> {
                        playerView?.showLoading()
                        detachWebRtcTracks()
                    }
                    else -> {}
                }
            }
    }

    private fun attachWebRtcTracks(video: VideoTrack?, audio: AudioTrack?) {
        if (isWebRtcFailed || !isWebRtcRendererInitialized || isWebRtcTracksAttached) return
        val view = playerView ?: return
        val owner = lifecycleOwner ?: return

        val attachmentId = ++webRtcAttachmentId
        val attachedAtMs = SystemClock.elapsedRealtime()
        videoTrack = video
        audioTrack = audio
        webRtcFirstFrameRendered = false
        isWebRtcTracksAttached = true

        view.showWebRtcSurface()
        view.showLoading()

        val renderer = view.binding.webRtcView
        renderer.disableFpsReduction()

        webRtcFrameListener?.let {
            runCatching { renderer.removeFrameListener(it) }
        }

        val listener = object : EglRenderer.FrameListener {
            override fun onFrame(bitmap: Bitmap?) {
                val renderedAtMs = SystemClock.elapsedRealtime()
                renderer.post {
                    if (attachmentId != webRtcAttachmentId || !isWebRtcTracksAttached || isWebRtcFailed || webRtcFirstFrameRendered) return@post
                    webRtcFrameListener = null
                    webRtcFirstFrameTimeoutJob?.cancel()
                    PlayerLogger.d("[WRTC-MEDIA] First frame rendered on webRtcView" +
                            " attachment=$attachmentId elapsedRealtimeMs=$renderedAtMs" +
                            " attachToRenderMs=${renderedAtMs - attachedAtMs}")
                    webRtcFirstFrameRendered = true
                    view.showStreamReady()
                }
            }
        }
        webRtcFrameListener = listener
        renderer.addFrameListener(listener, 0f)

        runCatching { videoTrack?.setEnabled(true) }
        runCatching { videoTrack?.addSink(renderer) }

        setupAudioControl(audioTrack != null)

        webRtcFirstFrameTimeoutJob?.cancel()
        webRtcFirstFrameTimeoutJob = owner.lifecycleScope.launch {
            delay(WEB_RTC_FF_TIMEOUT.milliseconds)
            if (attachmentId == webRtcAttachmentId && !webRtcFirstFrameRendered) {
                PlayerLogger.w("[FLOW] Fallback to LL-HLS (Reason: first frame waiting timeout)")
                detachWebRtcTracks()
                isWebRtcFailed = true
                runHls()
            }
        }
    }

    private fun createHlsPlayer() {
        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady(player: DefaultCCTVPlayer) {
                player.getPlayer()?.videoFormat?.let { format ->
                    PlayerLogger.d("[HLS] video format: ${format.width}x${format.height}")
                    if (format.width > 0 && format.height > 0) {
                        playerView?.setAspectRatio(format.width.toFloat() / format.height.toFloat())
                    }
                }
            }

            override fun onRenderFirstFrame() {
                if (isWebRtcFailed) playerView?.showStreamReady()
            }

            override fun onAudioAvailabilityChanged(isAvailable: Boolean) {
                if (isWebRtcFailed) setupAudioControl(isAvailable)
            }

            override fun onPlayerError(exception: Exception) {
                onErrorListener?.invoke(exception)
                handleHlsError(exception)
            }
        }

        val ctx = context
        hlsPlayer = when (config?.serverType) {
            MediaServerType.MACROSCOP -> MacroscopPlayer(ctx, callbacks)
            MediaServerType.FORPOST -> ForpostPlayer(ctx, callbacks)
            else -> DefaultCCTVPlayer(ctx, callbacks)
        }
    }

    private fun runHls() {
        val cfg = config ?: return
        val view = playerView ?: return
        val owner = lifecycleOwner ?: return

        playerNetworkJob?.cancel()
        view.showHlsSurface()
        releaseWebRtc()

        if (cfg.hlsUrl.isBlank()) {
            PlayerLogger.e(message = "[(LL)HLS] Preparation error: (Reason: HLS URL is blank)")
            view.showNoStream()
            return
        }

        isHlsFallbackActive = cfg.llHlsUrl.isBlank()
        llHlsRetryCount = 0
        hlsRetryCount = 0

        val mediaSource = mediaSourceFactory.createSource(cfg.llHlsUrl, cfg.hlsUrl)
        hlsPlayer?.setMediaSource(mediaSource, enableSound = cfg.enableSound)

        playerNetworkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.networkStatus.collectLatest { status ->
                    when (status) {
                        is NetworkStatus.Disconnected -> {
                            PlayerLogger.w("[NETWORK] Network lost. Stopping HLS...")
                            view.showLoading()
                            hlsPlayer?.stop()
                        }
                        is NetworkStatus.AvailableWiFi,
                        is NetworkStatus.AvailableCellular -> {
                            PlayerLogger.d("[NETWORK] Network available. Resuming HLS...")
                            view.showLoading()
                            hlsPlayer?.let { player ->
                                val exoPlayer = (player as? DefaultCCTVPlayer)?.getPlayer()
                                if (view.binding.playerView.player !== exoPlayer) {
                                    view.binding.playerView.player = exoPlayer
                                }
                                player.seekToDefaultPosition()
                                player.prepare()
                                player.play()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleHlsError(exception: Exception) {
        val cfg = config ?: return
        val playbackException = exception as? PlaybackException
        val errorCode = playbackException?.errorCode

        if (errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            PlayerLogger.w("[HLS] Behind live window caught! Recovering...")
            hlsPlayer?.stop()
            hlsPlayer?.seekToDefaultPosition()
            hlsPlayer?.prepare()
            hlsPlayer?.play()
            return
        }

        if (errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        ) {
            PlayerLogger.w("[HLS] Network timeout caught! Stopping...")
            playerView?.showLoading()
            hlsPlayer?.stop()
            return
        }

        val isPrimaryFailed = cfg.llHlsUrl.isNotBlank() && StreamFallbackCache.isFailed(cfg.llHlsUrl.toUri())
        val onFallback = isHlsFallbackActive || isPrimaryFailed

        if (!onFallback) {
            if (llHlsRetryCount < 3) {
                llHlsRetryCount++
                PlayerLogger.w("[(LL)HLS] LL-HLS retry attempt $llHlsRetryCount/3...")
                retryHls()
            } else {
                PlayerLogger.w("[(LL)HLS] LL-HLS failed. Falling back to standard HLS...")
                isHlsFallbackActive = true
                hlsRetryCount = 0
                if (cfg.llHlsUrl.isNotBlank()) StreamFallbackCache.markFailed(cfg.llHlsUrl.toUri())
                val fallbackSource = mediaSourceFactory.createSource("", cfg.hlsUrl)
                hlsPlayer?.setMediaSource(fallbackSource, enableSound = cfg.enableSound)
                retryHls()
            }
            return
        }

        if (hlsRetryCount < 3) {
            hlsRetryCount++
            PlayerLogger.w("[HLS] Standard HLS retry attempt $hlsRetryCount/3...")
            retryHls()
        } else {
            PlayerLogger.e(message = "[HLS] Fatal fail: Standard HLS failed after 3 retries")
            playerView?.showNoStream()
        }
    }

    private fun retryHls() {
        playerView?.showLoading()
        hlsPlayer?.stop()
        hlsPlayer?.seekToDefaultPosition()
        hlsPlayer?.prepare()
        hlsPlayer?.play()
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        muteButton?.isSelected = muted

        if (!isWebRtcFailed) {
            val audioEnabled = !muted
            runCatching {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager?.mode != AudioManager.MODE_NORMAL) {
                    audioManager?.mode = AudioManager.MODE_NORMAL
                }
            }
            runCatching { audioTrack?.setEnabled(audioEnabled) }
        } else {
            hlsPlayer?.isMuted = muted
        }
        onMuteChangedListener?.invoke(muted)
    }

    private fun setupAudioControl(isAvailable: Boolean) {
        val soundEnabled = config?.enableSound == true
        PlayerLogger.i("[FLOW] RBT enableSound: $soundEnabled")
        val audioAvailable = soundEnabled && isAvailable

        muteButton?.isVisible = audioAvailable
        if (!audioAvailable) return

        setMuted(true)
        muteButton?.setOnClickListener {
            setMuted(!isMuted)
        }
    }

    private fun detachWebRtcTracks() {
        PlayerLogger.i("[FLOW] detachWebRtcTracks called")
        ++webRtcAttachmentId
        webRtcFirstFrameTimeoutJob?.cancel()
        val view = playerView

        webRtcFrameListener?.let {
            if (view != null) runCatching { view.binding.webRtcView.removeFrameListener(it) }
            webRtcFrameListener = null
        }

        videoTrack?.let {
            if (view != null) runCatching { it.removeSink(view.binding.webRtcView) }
            runCatching { it.setEnabled(false) }
        }
        audioTrack?.let { runCatching { it.setEnabled(false) } }

        videoTrack = null
        audioTrack = null
        isWebRtcTracksAttached = false
        webRtcFirstFrameRendered = false

        if (isWebRtcRendererInitialized && view != null) {
            runCatching { view.binding.webRtcView.clearImage() }
        }

        muteButton?.isSelected = true
        muteButton?.setOnClickListener(null)
        muteButton?.isVisible = false
    }

    private fun releaseWebRtc() {
        if (!isWebRtcRendererInitialized) return
        detachWebRtcTracks()
        val view = playerView ?: return
        isWebRtcRendererInitialized = false
        PlayerLogger.i("[FLOW] Releasing WebRTC EGL renderer")
        runCatching { view.binding.webRtcView.release() }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (isWebRtcFailed) {
            hlsPlayer?.stop()
        } else {
            detachWebRtcTracks()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        detach()
    }

    fun detach() {
        playerNetworkJob?.cancel()
        webRtcFirstFrameTimeoutJob?.cancel()
        detachWebRtcTracks()
        releaseWebRtc()
        // Disconnect the surface and clear the UI reference before releasing the player.
        playerView?.binding?.playerView?.player = null
        hlsPlayer?.releasePlayer()
        hlsPlayer = null

        playerLogButton?.setOnClickListener(null)
        playerLogButton?.isVisible = false

        muteButton = null
        playerLogButton = null
        fragmentManager = null
        playerLogContainerId = null

        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = null
        playerView = null
    }
}
