package com.sesameware.domain.model.response

import android.os.Parcelable
import com.sesameware.domain.model.concatIfCorrectUrl
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

typealias CamMapResponse = ApiResult<List<CamMap>>?

@Parcelize
data class EntranceCamera(
    val previewUrl: String,
    val whepUrl: String = "",
    val hlsUrl: String
) : Parcelable {
    val isValid: Boolean
        get() = previewUrl.isNotBlank() || hlsUrl.isNotBlank() || hlsUrl.isNotBlank()
}

data class CamMap(
    @Json(name = "id") val domophoneId: Int,
    @Json(name = "entranceId") val entranceId: Int? = null,
    @Json(name = "url") val url: String,
    @Json(name = "token") val token: String,
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
    @Json(name = "altCameras") val altCameras: List<AltCameras>? = null  // additional cameras
) {
    val serverType: MediaServerType
        get() {
            return when(_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
                MediaServerType.MEDIA_TYPE_SESAMEWARE -> MediaServerType.SESAMEWARE
                else -> MediaServerType.FLUSSONIC
            }
        }

    val entranceCamera: EntranceCamera
        get() = EntranceCamera(
            previewUrl = when (serverType) {
                MediaServerType.NIMBLE -> concatIfCorrectUrl(url, "/thumbnail.mp4?wmsAuthSign=$token")
                MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
                MediaServerType.MACROSCOP,
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/preview.mp4?token=$token")
            },
            hlsUrl = when (serverType) {
                MediaServerType.NIMBLE -> concatIfCorrectUrl(url, "/playlist.m3u8?wmsAuthSign=$token")
                MediaServerType.MACROSCOP,
                MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/index.m3u8?token=$token")
            },
            whepUrl = when (serverType) {
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/whep?token=$token")
                MediaServerType.NIMBLE,
                MediaServerType.MACROSCOP,
                MediaServerType.FORPOST -> ""
            }
        )

    val additionalCameras: List<EntranceCamera>? = altCameras?.map { altCam ->
        EntranceCamera(
            previewUrl = when (altCam.serverType) {
                MediaServerType.NIMBLE -> concatIfCorrectUrl(url, "/thumbnail.mp4?wmsAuthSign=$token")
                MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
                MediaServerType.MACROSCOP,
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/preview.mp4?token=$token")
            },
            hlsUrl = when (altCam.serverType) {
                MediaServerType.NIMBLE -> concatIfCorrectUrl(url, "/playlist.m3u8?wmsAuthSign=$token")
                MediaServerType.MACROSCOP,
                MediaServerType.FORPOST -> concatIfCorrectUrl(url, "&$token")
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/index.m3u8?token=$token")
            },
            whepUrl = when (altCam.serverType) {
                MediaServerType.SESAMEWARE,
                MediaServerType.FLUSSONIC -> concatIfCorrectUrl(url, "/whep?token=$token")
                MediaServerType.NIMBLE,
                MediaServerType.MACROSCOP,
                MediaServerType.FORPOST -> ""
            }
        )
    }

    data class AltCameras(
        @Json(name = "cameraId") val cameraId: Int,
        @Json(name = "url") val url: String,
        @Json(name = "token") val token: String,
        @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC
    ) {
        val serverType: MediaServerType
            get() {
                return when(_serverType) {
                    MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                    MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                    MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
                    MediaServerType.MEDIA_TYPE_SESAMEWARE -> MediaServerType.SESAMEWARE
                    else -> MediaServerType.FLUSSONIC
                }
            }
    }
}
