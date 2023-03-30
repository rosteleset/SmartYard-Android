package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

abstract class BaseCCTVPlayer {
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun seekTo(position: Long)
    abstract fun currentPosition(): Long
    abstract fun mediaDuration(): Long
    abstract fun setPlaybackSpeed(speed: Float)
    abstract fun isReady(): Boolean
    abstract fun prepareMedia(mediaUrl: String?, from: Long = INVALID_POSITION, mediaDuration: Long = INVALID_DURATION, doPlay: Boolean = false)
    abstract fun releasePlayer()
    open var playWhenReady: Boolean = false

    interface Callbacks {
        fun onPlayerStateReady() {}
        fun onPlayerStateEnded() {}
        fun onPlayerStateBuffering() {}
        fun onPlayerStateIdle() {}
        fun onPlayerError(exception: Exception) {}
    }

    companion object {
        const val INVALID_POSITION = -1L
        const val INVALID_DURATION = -1L
    }
}

open class DefaultCCTVPlayer(private val context: Context, private val forceVideoTrack: Boolean, private val callbacks: Callbacks? = null) : BaseCCTVPlayer() {
    protected var mPlayer: SimpleExoPlayer? = null

    override var playWhenReady: Boolean
        get() = mPlayer?.playWhenReady == true
        set(value) {
            mPlayer?.playWhenReady = value
        }

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

    override fun prepareMedia(mediaUrl: String?, from: Long, mediaDuration: Long, doPlay: Boolean) {
        Timber.d("debug_dmm  mediaUrl = $mediaUrl")
        mPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
        mPlayer?.prepare()
        mPlayer?.playWhenReady = doPlay
    }

    override fun releasePlayer() {
        stop()
        mPlayer?.release()
        mPlayer = null
    }

    fun getPlayer() : SimpleExoPlayer? {
        return mPlayer
    }

    private fun createPlayer() {
        val trackSelector = DefaultTrackSelector(context)
        mPlayer  = SimpleExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
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

            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)

                callbacks?.onPlayerError(error)
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                super.onTracksChanged(trackGroups, trackSelections)

                if (!forceVideoTrack) {
                    return
                }

                val decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false, false)
                val maxSupportedWidth = (decoderInfo?.capabilities?.videoCapabilities?.supportedWidths?.upper ?: 0) * RESOLUTION_TOLERANCE
                val maxSupportedHeight = (decoderInfo?.capabilities?.videoCapabilities?.supportedHeights?.upper ?: 0) * RESOLUTION_TOLERANCE

                (mPlayer?.trackSelector as? DefaultTrackSelector)?.let{ trackSelector ->
                    trackSelector.currentMappedTrackInfo?.let { mappedTrackInfo ->
                        for (k in 0 until mappedTrackInfo.rendererCount) {
                            if (mappedTrackInfo.getRendererType(k) == C.TRACK_TYPE_VIDEO) {
                                val rendererTrackGroups = mappedTrackInfo.getTrackGroups(k)
                                for (i in 0 until rendererTrackGroups.length) {
                                    val tracks = mutableListOf<Int>()
                                    for (j in 0 until rendererTrackGroups[i].length) {
                                        if (mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_HANDLED ||
                                            mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_EXCEEDS_CAPABILITIES &&
                                            (maxSupportedWidth >= rendererTrackGroups[i].getFormat(j).width ||
                                                    maxSupportedHeight >= rendererTrackGroups[i].getFormat(j).height)) {
                                            tracks.add(j)
                                        }
                                    }
                                    val selectionOverride = DefaultTrackSelector.SelectionOverride(i, *tracks.toIntArray())
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
        })
    }

    companion object {
        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
    }
}

class MacroscopPlayer(context: Context, forceVideoTrack: Boolean, callbacks: Callbacks? = null)
    : DefaultCCTVPlayer(context, forceVideoTrack, callbacks), CoroutineScope by MainScope() {

    private var fromUtc: Long = INVALID_POSITION
    private var duration: Long = INVALID_DURATION
    private var startOffset: Long = 0L

    override fun seekTo(position: Long) {
    }

    override fun currentPosition(): Long {
        return super.currentPosition()
    }

    override fun mediaDuration(): Long {
        return super.mediaDuration()
    }

    override fun prepareMedia(mediaUrl: String?, from: Long, mediaDuration: Long, doPlay: Boolean) {
        mediaUrl?.let { url ->
            startOffset = 0L
            if (from == INVALID_POSITION || mediaDuration == INVALID_DURATION) {
                fromUtc = INVALID_POSITION
                duration = INVALID_DURATION
            } else {
                fromUtc = from
                duration = mediaDuration
            }
            launch(Dispatchers.IO) {
                val builder = OkHttpClient.Builder()
                with(builder) {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
                val client = builder.build()
                var requestUrl = url
                if (from != INVALID_POSITION) {
                    val startTime = Instant.ofEpochSecond(from).atOffset(ZoneOffset.UTC).format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                    requestUrl += "&starttime=${startTime}&mode=archive&isForward=true&speed=1&sound=off"
                }
                Timber.d("__D__  requestUrl = $requestUrl")
                val request = Request.Builder()
                    .url(requestUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val p = url.indexOf("?")
                        if (p >= 0) {
                            val r = response.body!!.string()
                            Timber.d("__D__  response = $r")
                            val realHlsUrl = url.substring(0, p) + "/" + r
                            withContext(Dispatchers.Main) {
                                super.prepareMedia(realHlsUrl, INVALID_POSITION, INVALID_DURATION, doPlay)
                            }
                        }
                    }
                }
            }
        }
    }
}