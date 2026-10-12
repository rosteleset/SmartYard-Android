package com.sesameware.domain.model.mapper

import com.sesameware.domain.model.concatIfCorrectUrl
import com.sesameware.domain.model.response.CamMap
import com.sesameware.domain.model.response.EntranceCamera
import com.sesameware.domain.model.response.MediaServerType


internal fun CamMap.toEntranceCamera(): EntranceCamera {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return EntranceCamera(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = "$previewUrl$timestamp"
    )
}

internal fun CamMap.AltCamera.toEntranceCamera(timestamp: Long): EntranceCamera {
    val previewUrl = resolvePreviewUrl(url, token, serverType)
    return EntranceCamera(
        previewUrl = previewUrl,
        whepUrl = resolveWhepUrl(url, token, serverType),
        llHlsUrl = resolveLlHlsUrl(url, token, serverType),
        hlsUrl = resolveHlsUrl(url, token, serverType),
        enableSound = hasSound,
        previewCacheKey = "$previewUrl$timestamp"
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

