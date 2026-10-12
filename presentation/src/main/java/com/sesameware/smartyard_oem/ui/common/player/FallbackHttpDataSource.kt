package com.sesameware.smartyard_oem.ui.common.player

import android.net.Uri
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.sesameware.data.player.log.PlayerLogger
import java.io.IOException

class FallbackHttpDataSource(
    private val upstream: HttpDataSource,
    private val primaryUri: Uri,
    private val fallbackUri: Uri
) : HttpDataSource by upstream {

    private var activeUri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        val isPrimary = (dataSpec.uri == primaryUri)

        if (isPrimary && StreamFallbackCache.isFailed(primaryUri)) {
            return openFallback(dataSpec)
        }

        if (isPrimary) {
            try {
                activeUri = primaryUri
                PlayerLogger.d("[EXO-FBDS] Starting primaryUri: $primaryUri")
                return upstream.open(dataSpec)
            } catch (e: InvalidResponseCodeException) {
                if (e.responseCode in 403..404) {
                    PlayerLogger.w(
                        "[EXO-FBDS] Primary data source failed with ${e.responseCode}: " +
                                "throwing exception for retry/restart"
                    )
                    throw e
                }
                val code = e.responseCode
                val msg = e.message
                PlayerLogger.w(
                    "[EXO-FBDS] Primary data source failed with invalid response: $code - $msg"
                )
                StreamFallbackCache.markFailed(primaryUri)
                return openFallback(dataSpec)
            } catch (e: IOException) {
                PlayerLogger.w(
                    "[EXO-FBDS] Primary data source failed with IO exception: ${e.cause} - ${e.message}"
                )
                StreamFallbackCache.markFailed(primaryUri)
                return openFallback(dataSpec)
            }
        }

        return upstream.open(dataSpec)
    }

    private fun openFallback(dataSpec: DataSpec): Long {
        runCatching { upstream.close() }
        activeUri = fallbackUri
        PlayerLogger.w("[EXO-FBDS] Fallback triggered: switching to fallbackUri: $fallbackUri")
        val fallbackDataSpec = dataSpec.buildUpon()
            .setUri(fallbackUri)
            .build()
        return upstream.open(fallbackDataSpec)
    }

    override fun getUri(): Uri? = activeUri ?: upstream.uri
}