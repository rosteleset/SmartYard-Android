package ru.madbrains.smartyard.ui.player.classes

import android.content.Context
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.cctv_video.ZoomLayout
import timber.log.Timber

class VideoPlayer(val context: Context) {
    var player: ExoPlayer? = null


    init {
        createPlayer()
    }


    private fun createPlayer() {
        player = ExoPlayer.Builder(context).build()
        player?.playWhenReady = true
        player?.volume = 0f
    }

    private fun muteVideo(){
        if (player?.volume == 0f){
            player?.volume = 1f
        }else{
            player?.volume = 0f
        }
    }

    fun isMuted(): Boolean = player?.volume == 0f

    fun changeVideoSource(url: String) {
        if (url.isNotEmpty()) {
            player.let { player ->
                player?.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                player?.prepare()
            }
        }
    }

    fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    fun stop(){
        player?.stop()
    }

    fun onResume(){
        try {
            player?.prepare()
            player?.play()
        } catch (e: PlaybackException) {
            e.printStackTrace()
        }
    }
}