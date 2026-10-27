package com.sesameware.smartyard_oem.ui.common.player

import com.sesameware.domain.model.concatIfCorrectUrl
import com.sesameware.domain.model.response.CCTVCityCameraData
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.CamMap
import com.sesameware.domain.model.response.MediaServerType
import kotlin.random.Random

internal fun CCTVData.toStreamSourceConfig(): StreamSourceConfig {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return StreamSourceConfig(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = previewCacheKey,
        serverType = serverType
    )
}

internal fun CCTVCityCameraData.toStreamSourceConfig(): StreamSourceConfig {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return StreamSourceConfig(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = Random.nextInt().toString(),
        serverType = serverType
    )
}

private fun CamMap.toSingleStreamSourceConfig(): StreamSourceConfig {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return StreamSourceConfig(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = "$previewUrl$timestamp",
        serverType = serverType
    )
}

internal fun CamMap.toStreamSourceConfigs(): List<StreamSourceConfig> =
    mutableListOf(this.toSingleStreamSourceConfig()).apply {
        val addCams = altCameras?.map { it.toStreamSourceConfig(timestamp) }.orEmpty()
        addAll(addCams)
    }.toList()

internal fun CamMap.AltCamera.toStreamSourceConfig(timestamp: Long): StreamSourceConfig {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return StreamSourceConfig(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = "$previewUrl$timestamp",
        serverType = serverType
    )
}

private fun resolvePreviewUrl(url: String, token: String, serverType: MediaServerType): String =
    when (serverType) {
        MediaServerType.NIMBLE -> concatIfCorrectUrl(url, "/thumbnail.mp4?wmsAuthSign=$token")
        MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
        MediaServerType.MACROSCOP,
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/preview.mp4?token=$token")
    }

private fun resolveWhepUrl(url: String, token: String, serverType: MediaServerType): String =
    when (serverType) {
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/whep?token=$token")
        MediaServerType.NIMBLE,
        MediaServerType.MACROSCOP,
        MediaServerType.FORPOST -> ""
    }

private fun resolveLlHlsUrl(url: String, token: String, serverType: MediaServerType): String =
    when (serverType) {
        MediaServerType.NIMBLE -> concatIfCorrectUrl(url,"/playlist.m3u8?wmsAuthSign=$token")
        MediaServerType.MACROSCOP,
        MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/index.ll.m3u8?token=$token")
    }

private fun resolveHlsUrl(url: String, token: String, serverType: MediaServerType): String =
    when (serverType) {
        MediaServerType.NIMBLE -> concatIfCorrectUrl(url,"/playlist.m3u8?wmsAuthSign=$token")
        MediaServerType.MACROSCOP,
        MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/index.m3u8?token=$token")
    }

