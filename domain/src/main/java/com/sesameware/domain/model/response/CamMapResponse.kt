package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias CamMapResponse = ApiResult<List<CamMap>>?

data class CamMap(
    @param:Json(name = "id") val domophoneId: Int,
    @param:Json(name = "entranceId") val entranceId: Int? = null,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "token") val token: String,
    @param:Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
    @param:Json(name = "hasSound") val _hasSound: Boolean?,
    @param:Json(name = "altCameras") val altCameras: List<AltCamera>? = null  // additional cameras
) {

    val timestamp: Long = System.currentTimeMillis()

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

    val hasSound: Boolean
        get() = _hasSound ?: true

    data class AltCamera(
        @param:Json(name = "cameraId") val cameraId: Int,
        @param:Json(name = "url") val url: String,
        @param:Json(name = "token") val token: String,
        @param:Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
        @param:Json(name = "hasSound") val _hasSound: Boolean?
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

        val hasSound: Boolean
            get() = _hasSound ?: true
    }
}
