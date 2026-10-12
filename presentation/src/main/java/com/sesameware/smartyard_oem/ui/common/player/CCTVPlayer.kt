package com.sesameware.smartyard_oem.ui.common.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.video.VideoSize
import com.sesameware.data.DataModule
import com.sesameware.data.player.log.PlayerLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

abstract class BaseCCTVPlayer {
    open var isMuted: Boolean = true
    open var soundEnabled: Boolean = true
    abstract fun prepare()
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun seekTo(position: Long)
    abstract fun seekToDefaultPosition()
    abstract fun setMediaSource(mediaSource: MediaSource, enableSound: Boolean = soundEnabled)
    abstract fun currentPosition(): Long
    abstract fun mediaDuration(): Long
    abstract fun setPlaybackSpeed(speed: Float)
    abstract fun isReady(): Boolean
    abstract fun isPlaying(): Boolean
    abstract fun isEnded(): Boolean
    abstract fun isIdle(): Boolean
    abstract fun prepareMedia(mediaUrl: String?, from: Long = INVALID_POSITION, mediaDuration: Long = INVALID_DURATION, seekMediaTo: Long = 0L, doPlay: Boolean = false, enableSound: Boolean)
    abstract fun releasePlayer()
    open var playWhenReady: Boolean = false

    interface Callbacks {
        fun onPlayerStateReady(player: DefaultCCTVPlayer) {}
        fun onPlayerStateEnded() {}
        fun onPlayerStateBuffering() {}
        fun onPlayerStateIdle() {}
        fun onPlayerError(exception: Exception) {}
        fun onRenderFirstFrame() {}
        fun onVideoSizeChanged(videoSize: VideoSize) {}
        fun onAudioAvailabilityChanged(isAvailable: Boolean) {}
        fun onAbrTrackChanged(resolution: String, bitrate: Int) {}
    }

    companion object {
        const val INVALID_POSITION = -1L
        const val INVALID_DURATION = -1L
    }
}

open class DefaultCCTVPlayer(
    private val context: Context,
    protected val callbacks: Callbacks? = null
) : BaseCCTVPlayer() {

    override var isMuted: Boolean = false
        set(value) {
            field = value
            mPlayer?.volume = if (value) 0.0f else 1.0f
            PlayerLogger.d("[EXO] Muted state changed to: $value")
        }

    override var soundEnabled: Boolean = true

    protected var mPlayer: ExoPlayer? = null

    override var playWhenReady: Boolean
        get() = mPlayer?.playWhenReady == true
        set(value) {
            mPlayer?.playWhenReady = value
        }

    init {
        createPlayer()
    }

    override fun prepare() {
        PlayerLogger.i("[EXO] Prepare called")
        mPlayer?.prepare()
    }

    override fun play() {
        PlayerLogger.i("[EXO] Play called")
        mPlayer?.play()
    }

    override fun pause() {
        PlayerLogger.i("[EXO] Pause called")
        mPlayer?.pause()
    }

    override fun stop() {
        PlayerLogger.i("[EXO] Stop called")
        mPlayer?.stop()
    }

    override fun seekTo(position: Long) {
        PlayerLogger.i("[EXO] SeekTo $position ms called")
        mPlayer?.seekTo(position)
    }

    override fun seekToDefaultPosition() {
        PlayerLogger.i("[EXO] SeekToDefaultPosition called")
        mPlayer?.seekToDefaultPosition()
    }

    override fun setMediaSource(mediaSource: MediaSource, enableSound: Boolean) {
        soundEnabled = enableSound
        PlayerLogger.i("[EXO] SetMediaSource called")
        mPlayer?.setMediaSource(mediaSource)
    }

    override fun currentPosition(): Long = mPlayer?.currentPosition ?: INVALID_POSITION
    override fun mediaDuration(): Long = mPlayer?.duration ?: INVALID_DURATION
    override fun setPlaybackSpeed(speed: Float) {
        mPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    override fun isReady(): Boolean = mPlayer?.playbackState == Player.STATE_READY
    override fun isPlaying(): Boolean = mPlayer?.isPlaying == true
    override fun isEnded(): Boolean = mPlayer?.playbackState == Player.STATE_ENDED
    override fun isIdle(): Boolean = mPlayer?.playbackState == Player.STATE_IDLE

    override fun prepareMedia(
        mediaUrl: String?,
        from: Long,
        mediaDuration: Long,
        seekMediaTo: Long,
        doPlay: Boolean,
        enableSound: Boolean
    ) {
        soundEnabled = enableSound
        PlayerLogger.d("[EXO] Preparing media. URL: $mediaUrl")
        mPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
        mPlayer?.prepare()
        mPlayer?.playWhenReady = doPlay
        if (seekMediaTo > 0) {
            PlayerLogger.d("[EXO] Seeking to initial position: $seekMediaTo ms")
            mPlayer?.seekTo(seekMediaTo)
        }
    }

    override fun releasePlayer() {
        PlayerLogger.i("[EXO] Releasing EXO player")
        stop()
        mPlayer?.release()
        mPlayer = null
    }

    fun getPlayer(): ExoPlayer? = mPlayer

    private fun createPlayer() {
        PlayerLogger.i("[EXO] createPlayer called")
        val trackSelector = DefaultTrackSelector(context)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 50000, 1500, 2000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        mPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
        mPlayer?.volume = 0.0f
//        if (BuildConfig.DEBUG) mPlayer?.addAnalyticsListener(EventLogger())

        mPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                when (state) {
                    Player.STATE_READY -> {
                        PlayerLogger.d("[EXO] State: READY (Playing: ${mPlayer?.playWhenReady})")
                        callbacks?.onPlayerStateReady(this@DefaultCCTVPlayer)
                    }

                    Player.STATE_ENDED -> {
                        PlayerLogger.i("[EXO] State: ENDED")
                        callbacks?.onPlayerStateEnded()
                    }

                    Player.STATE_BUFFERING -> {
                        PlayerLogger.i("[EXO] State: BUFFERING...")
                        callbacks?.onPlayerStateBuffering()
                    }

                    Player.STATE_IDLE -> {
                        PlayerLogger.i("[EXO] State: IDLE")
                        callbacks?.onPlayerStateIdle()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                val cause = error.cause
                val rawMsg = cause?.message ?: error.message
                val cleanMsg = rawMsg?.lineSequence()?.firstOrNull()?.substringBefore("at ")?.trim() ?: "Unknown error"
                val errorMsg = "[EXO] Player error [${error.errorCodeName}]: $cleanMsg"
                PlayerLogger.e(message = errorMsg)

                if (error is ExoPlaybackException && cause is HttpDataSource.HttpDataSourceException) {
                    PlayerLogger.e(message = "[EXO] HTTP Error. Code: ${error.errorCode}")
                }

                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    PlayerLogger.e(message = "[EXO] BehindLiveWindowException! Player needs restart.")
                }

                callbacks?.onPlayerError(error)
            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)

                val hasAudio = soundEnabled && containsAudioTrack(tracks.groups)
                PlayerLogger.d("[EXO] Tracks changed. Sound enabled: $soundEnabled" +
                        if (soundEnabled) " Audio available: $hasAudio" else "")
                callbacks?.onAudioAvailabilityChanged(hasAudio)

                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                val format = group.getTrackFormat(i)
                                val resolution = "${format.width}x${format.height}"
                                val bitrate = format.bitrate
                                PlayerLogger.i("[EXO] ABR Video Track Selected: $resolution, Bitrate: ${bitrate / 1000} kbps")
                                callbacks?.onAbrTrackChanged(resolution, bitrate)
                            }
                        }
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                PlayerLogger.d("[EXO] First frame rendered (TTFF)")
                callbacks?.onRenderFirstFrame()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                PlayerLogger.d("[EXO] Video size changed to: ${videoSize.width}x${videoSize.height}")
                callbacks?.onVideoSizeChanged(videoSize)
            }
        })
    }

    private fun containsAudioTrack(trackGroups: List<Tracks.Group>): Boolean {
        return trackGroups.any { group ->
            (0 until group.length).any { i ->
                MimeTypes.isAudio(group.getTrackFormat(i).sampleMimeType)
            }
        }
    }
}

abstract class NoDurationPlayer(context: Context, callbacks: Callbacks? = null)
    : DefaultCCTVPlayer(context, callbacks), CoroutineScope by MainScope() {

    protected var httpClient: OkHttpClient

    protected var fromUtc: Long = INVALID_POSITION
    protected var duration: Long = INVALID_DURATION
    protected var currentMediaPosition = 0L
    protected var currentSegmentStart: Long = 0L
    protected var currentSegmentTimePlayed: Long = 0L
    protected var internalCurrentPosition: Long = 0L

    protected var mUrl: String = ""
    protected var progressTimer: Timer? = null
    protected var isNewTimeline = false
    protected var processSeeking = false

    init {
        Timber.d("__P__ init called")
        mPlayer?.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                super.onTimelineChanged(timeline, reason)

                //Timber.d("__P__  onTimelineChanged    currentPosition = ${mPlayer?.currentPosition}")
                isNewTimeline = true
                internalCurrentPosition = mPlayer?.currentPosition ?: 0L
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                if (fromUtc != INVALID_POSITION && state == Player.STATE_READY && progressTimer == null) {
                    progressTimer = fixedRateTimer("playerTimer", false, 0, 300) {
                        launch(Dispatchers.Main) {
                            mPlayer?.let { player ->
                                //Timber.d("__P__ progressTimer    ${player.currentPosition}      $internalCurrentPosition     $isNewTimeline")
                                if (player.currentPosition > internalCurrentPosition && !isNewTimeline) {
                                    val delta = player.currentPosition - internalCurrentPosition
                                    currentSegmentTimePlayed += delta
                                    currentMediaPosition = currentSegmentStart + currentSegmentTimePlayed
                                    if (currentMediaPosition > duration) {
                                        player.stop()
                                        clearProgressTimer()
                                    }
                                }
                                internalCurrentPosition = player.currentPosition
                                isNewTimeline = false
                            }
                        }
                    }
                } else if (mPlayer?.isPlaying == false) {
                    clearProgressTimer()
                }
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()

                processSeeking = false
            }
        })

        val builder = OkHttpClient.Builder()
        with(builder) {
            connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        }
        httpClient = builder.build()
    }

    override fun seekTo(position: Long) {
        Timber.d("__P__   call seekTo position = $position    playWhenReady = ${mPlayer?.playWhenReady}")

        if (processSeeking) {
            return
        }

        processSeeking = true
        val doPlay = mPlayer?.playWhenReady
        //mPlayer?.stop()
        clearProgressTimer()

        currentMediaPosition = position
        currentSegmentStart = position
        currentSegmentTimePlayed = 0L
        setMediaSeek(doPlay ?: false)
    }

    override fun currentPosition(): Long {
        //Timber.d("__P__   currentPosition = $currentMediaPosition")
        return currentMediaPosition
    }

    override fun mediaDuration(): Long {
        return duration
    }

    override fun releasePlayer() {
        clearProgressTimer()
        super.releasePlayer()
    }

    private fun clearProgressTimer() {
        progressTimer?.cancel()
        progressTimer = null
    }

    abstract fun setMediaSeek(doPlay: Boolean = false)

    companion object {
        const val REQUEST_TIMEOUT = 10L  // in seconds
    }
}

class MacroscopPlayer(context: Context, callbacks: Callbacks? = null)
    : NoDurationPlayer(context, callbacks) {
    override fun prepareMedia(
        mediaUrl: String?,
        from: Long,
        mediaDuration: Long,
        seekMediaTo: Long,
        doPlay: Boolean,
        enableSound: Boolean
    ) {
        soundEnabled = enableSound
        mediaUrl?.let { url ->
            currentSegmentStart = seekMediaTo
            currentMediaPosition = currentSegmentStart
            currentSegmentTimePlayed = 0L
            if (from == INVALID_POSITION || mediaDuration == INVALID_DURATION) {
                fromUtc = INVALID_POSITION
                duration = INVALID_DURATION
            } else {
                fromUtc = from * 1000
                duration = mediaDuration * 1000
            }
            mUrl = url

            setMediaSeek(doPlay, )
        }
    }

    override fun setMediaSeek(doPlay: Boolean) {
        Timber.d("__P__    call setMediaSeek")
        launch(Dispatchers.IO) {
            var requestUrl = mUrl
            if (fromUtc != INVALID_POSITION) {
                val startTime = Instant.ofEpochSecond((fromUtc + currentSegmentStart) / 1000).atOffset(ZoneOffset.UTC).format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                requestUrl += "&starttime=${startTime}&mode=archive&isForward=true&speed=1&sound=off"
            }
            val request = Request.Builder()
                .url(requestUrl)
                .build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val p = mUrl.indexOf("?")
                        if (p >= 0) {
                            val r = response.body!!.string()
                            val realHlsUrl = mUrl.substring(0, p) + "/" + r
                            withContext(Dispatchers.Main) {
                               super.prepareMedia(
                                   mediaUrl = realHlsUrl,
                                   from = INVALID_POSITION,
                                   mediaDuration = INVALID_DURATION,
                                   seekMediaTo = 0L,
                                   doPlay = doPlay,
                                   enableSound = soundEnabled
                               )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    this@MacroscopPlayer.callbacks?.onPlayerError(e)
                }
            }
        }
    }
}

class ForpostPlayer(context: Context, callbacks: Callbacks? = null)
    : NoDurationPlayer(context, callbacks) {
    override fun prepareMedia(
        mediaUrl: String?,
        from: Long,
        mediaDuration: Long,
        seekMediaTo: Long,
        doPlay: Boolean,
        enableSound: Boolean
    ) {
        soundEnabled = enableSound
        mediaUrl?.let { url ->
            currentSegmentStart = seekMediaTo
            currentMediaPosition = currentSegmentStart
            currentSegmentTimePlayed = 0L
            if (from == INVALID_POSITION || mediaDuration == INVALID_DURATION) {
                fromUtc = INVALID_POSITION
                duration = INVALID_DURATION
            } else {
                fromUtc = from * 1000
                duration = mediaDuration * 1000
            }
            mUrl = url

            setMediaSeek(doPlay)
        }
    }

    override fun setMediaSeek(doPlay: Boolean) {
        Timber.d("__P__    call setMediaSeek")
        launch(Dispatchers.IO) {
            val bodyBuilder = FormBody.Builder()
            mUrl.toHttpUrlOrNull()?.let { forpostUrl ->
                for (i in 0 until forpostUrl.querySize) {
                    bodyBuilder.add(forpostUrl.queryParameterName(i), forpostUrl.queryParameterValue(i) ?: "")
                }
                if (fromUtc != INVALID_POSITION) {
                    bodyBuilder.add("TS", ((fromUtc + currentSegmentStart) / 1000).toString())
                    bodyBuilder.add("TZ", Instant.now().atZone(ZoneId.of(DataModule.serverTz)).offset.totalSeconds.toString())
                }
                val request = Request.Builder()
                    .url(forpostUrl.toString().split("?", limit = 1)[0])
                    .post(bodyBuilder.build())
                    .build()

                try {
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = JSONObject(response.body!!.string())
                            val realHlsUrl = json.optString("URL", "")
                            withContext(Dispatchers.Main) {
                                super.prepareMedia(
                                    mediaUrl = realHlsUrl,
                                    from = INVALID_POSITION,
                                    mediaDuration = INVALID_DURATION,
                                    seekMediaTo = 0L,
                                    doPlay = doPlay,
                                    enableSound = soundEnabled
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        this@ForpostPlayer.callbacks?.onPlayerError(e)
                    }
                }
            }
        }
    }
}
