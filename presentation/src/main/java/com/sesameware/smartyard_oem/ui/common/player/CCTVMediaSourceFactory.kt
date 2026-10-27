package com.sesameware.smartyard_oem.ui.common.player

import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource

class FallbackMediaSourceFactory(
    private val baseHttpFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
) {
    fun createSource(primaryUrl: String, fallbackUrl: String): MediaSource {
        if (primaryUrl.isBlank() || primaryUrl == fallbackUrl) {
            val cameraDataSourceFactory = DataSource.Factory {
                baseHttpFactory.createDataSource()
            }
            return HlsMediaSource.Factory(cameraDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(fallbackUrl.toUri()))
        }

        val primaryUri = primaryUrl.toUri()
        val fallbackUri = fallbackUrl.toUri()

        val cameraDataSourceFactory = DataSource.Factory {
            val upstream = baseHttpFactory.createDataSource()
            FallbackHttpDataSource(upstream, primaryUri, fallbackUri)
        }

        return HlsMediaSource.Factory(cameraDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(primaryUri))
    }
}