package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.video.VideoSize
import com.sesameware.data.DataModule
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
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun seekTo(position: Long)
    abstract fun currentPosition(): Long
    abstract fun mediaDuration(): Long
    abstract fun setPlaybackSpeed(speed: Float)
    abstract fun isReady(): Boolean
    abstract fun isPlaying(): Boolean
    abstract fun isEnded(): Boolean
    abstract fun isIdle(): Boolean
    abstract fun mute()
    abstract fun unMute()
    abstract fun prepareMedia(mediaUrl: String?, from: Long = INVALID_POSITION, mediaDuration: Long = INVALID_DURATION, seekMediaTo: Long = 0L, doPlay: Boolean = false)
    abstract fun getPlayer(): ExoPlayer?
    abstract fun releasePlayer()
    open var playWhenReady: Boolean = false

    interface Callbacks {
        fun onPlayerStateReady() {}
        fun onPlayerStateEnded() {}
        fun onPlayerStateBuffering() {}
        fun onPlayerStateIdle() {}
        fun onPlayerError(exception: Exception) {}
        fun onRenderFirstFrame() {}
        fun onVideoSizeChanged(videoSize: VideoSize) {}
        fun onAudioAvailabilityChanged(isAvailable: Boolean) {}
    }

    companion object {
        const val INVALID_POSITION = -1L
        const val INVALID_DURATION = -1L
    }
}

open class DefaultCCTVPlayer(private val context: Context, private val forceVideoTrack: Boolean, protected val callbacks: Callbacks? = null) : BaseCCTVPlayer() {
    protected var mPlayer: ExoPlayer? = null

    override var playWhenReady: Boolean
        get() = mPlayer?.playWhenReady == true
        set(value) {
            mPlayer?.playWhenReady = value
        }

    private var mCurrentVolume: Float = 1.0f

    init {
        createPlayer()
    }

    override fun play() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun seekTo(position: Long) {
        mPlayer?.seekTo(position)
    }

    override fun currentPosition(): Long {
        return mPlayer?.currentPosition ?: INVALID_POSITION
    }

    override fun mediaDuration(): Long {
        return mPlayer?.duration ?: INVALID_DURATION
    }

    override fun setPlaybackSpeed(speed: Float) {
        mPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    override fun isReady(): Boolean {
        return mPlayer?.playbackState == Player.STATE_READY
    }

    override fun isPlaying(): Boolean {
        return mPlayer?.isPlaying == true
    }

    override fun isEnded(): Boolean {
        return mPlayer?.playbackState == Player.STATE_ENDED
    }

    override fun isIdle(): Boolean {
        return mPlayer?.playbackState == Player.STATE_IDLE
    }

    override fun mute() {
        mPlayer?.let {
            if (it.volume != 0.0f) mCurrentVolume = it.volume
        }
        mPlayer?.volume = 0f
        Timber.d("__P__   Muting. Current volume = $mCurrentVolume. Player volume = ${mPlayer?.volume}")
    }

    override fun unMute() {
        mPlayer?.volume = mCurrentVolume
        Timber.d("__P__   Unmuting. Current volume = $mCurrentVolume. Player volume = ${mPlayer?.volume}")
    }

    override fun prepareMedia(mediaUrl: String?, from: Long, mediaDuration: Long, seekMediaTo: Long, doPlay: Boolean) {
        Timber.d("debug_dmm  mediaUrl = $mediaUrl")
        mute()
        mPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
        mPlayer?.prepare()
        mPlayer?.playWhenReady = doPlay
        if (seekMediaTo > 0) {
            mPlayer?.seekTo(seekMediaTo)
        }
    }

    override fun releasePlayer() {
        stop()
        mPlayer?.release()
        mPlayer = null
    }

    override fun getPlayer() : ExoPlayer? {
        return mPlayer
    }


    private fun createPlayer() {
        Timber.d("__P__ createPlayer called")
        val trackSelector = DefaultTrackSelector(context)
        mPlayer  = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
        mPlayer?.addAnalyticsListener(EventLogger())
        mPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                if (state == Player.STATE_READY) {
                    callbacks?.onPlayerStateReady()
                }

                if (state == Player.STATE_ENDED) {
                    callbacks?.onPlayerStateEnded()
                }

                if (state == Player.STATE_BUFFERING) {
                    callbacks?.onPlayerStateBuffering()
                }

                if (state == Player.STATE_IDLE) {
                    callbacks?.onPlayerStateIdle()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                Timber.d("__P__  Player error: ${error.errorCode} - ${error.message}")
                callbacks?.onPlayerError(error)
            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)

                callbacks?.onAudioAvailabilityChanged(containsAudioTrack(tracks.groups))

                if (!forceVideoTrack) {
                    return
                }

                forcePlayableTracksSelection()
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()

                Timber.d("__P__  Player rendered first frame")
                callbacks?.onRenderFirstFrame()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)

                Timber.d("__P__  Player video size changed")
                callbacks?.onVideoSizeChanged(videoSize)
            }
        })
    }

    private fun containsAudioTrack(trackGroups: List<Tracks.Group>): Boolean {
        Timber.d("__P__  Player searching audio track")
        trackGroups.forEach { group ->
            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                if (MimeTypes.isAudio(trackGroup.getFormat(i).sampleMimeType)) {
                    return true
                }
            }
        }
        return false
    }

    // ExoPlayer не всегда подбирает читаемый трек из набора треков разного разрешения для одного видео
    // Метод вручную собирает набор подходящих треков
    private fun forcePlayableTracksSelection() {
        Timber.d("__P__  Player selecting playable tracks")
        val decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false, false)
        val maxSupportedWidth =
            (decoderInfo?.capabilities?.videoCapabilities?.supportedWidths?.upper ?: 0) * RESOLUTION_TOLERANCE
        val maxSupportedHeight =
            (decoderInfo?.capabilities?.videoCapabilities?.supportedHeights?.upper ?: 0) * RESOLUTION_TOLERANCE

        (mPlayer?.trackSelector as? DefaultTrackSelector)?.let { trackSelector ->
            trackSelector.currentMappedTrackInfo?.let { mappedTrackInfo ->
                for (k in 0 until mappedTrackInfo.rendererCount) {
                    if (mappedTrackInfo.getRendererType(k) == C.TRACK_TYPE_VIDEO) {
                        val rendererTrackGroups = mappedTrackInfo.getTrackGroups(k)
                        for (i in 0 until rendererTrackGroups.length) {
                            val tracks2 = mutableListOf<Int>()
                            for (j in 0 until rendererTrackGroups[i].length) {
                                if (mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_HANDLED ||
                                    mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_EXCEEDS_CAPABILITIES &&
                                    (maxSupportedWidth >= rendererTrackGroups[i].getFormat(j).width ||
                                            maxSupportedHeight >= rendererTrackGroups[i].getFormat(j).height)
                                ) {
                                    tracks2.add(j)
                                }
                            }
                            val selectionOverride = DefaultTrackSelector.SelectionOverride(i, *tracks2.toIntArray())
                            @Suppress("DEPRECATION")
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setSelectionOverride(k, rendererTrackGroups, selectionOverride)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
    }
}

abstract class NoDurationPlayer(context: Context, forceVideoTrack: Boolean, callbacks: Callbacks? = null)
    : DefaultCCTVPlayer(context, forceVideoTrack, callbacks), CoroutineScope by MainScope() {

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

class MacroscopPlayer(context: Context, forceVideoTrack: Boolean, callbacks: Callbacks? = null)
    : NoDurationPlayer(context, forceVideoTrack, callbacks) {
    override fun prepareMedia(mediaUrl: String?, from: Long, mediaDuration: Long, seekMediaTo: Long, doPlay: Boolean) {
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
                               super.prepareMedia(realHlsUrl, INVALID_POSITION, INVALID_DURATION, 0L, doPlay)
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

class ForpostPlayer(context: Context, forceVideoTrack: Boolean, callbacks: Callbacks? = null)
    : NoDurationPlayer(context, forceVideoTrack, callbacks) {
    override fun prepareMedia(mediaUrl: String?, from: Long, mediaDuration: Long, seekMediaTo: Long, doPlay: Boolean) {
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
                            val realHls = json.optString("URL", "")
                            withContext(Dispatchers.Main) {
                                super.prepareMedia(realHls, INVALID_POSITION, INVALID_DURATION,0, doPlay)
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
