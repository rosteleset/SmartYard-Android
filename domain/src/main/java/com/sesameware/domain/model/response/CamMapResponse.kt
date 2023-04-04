package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias CamMapResponse = ApiResult<List<CamMap>>?

data class CamMap(
    @Json(name = "id") val id: Int,
    @Json(name = "url") val url: String,
    @Json(name = "token") val token: String,
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC
) {
    val serverType: MediaServerType
        get() {
            return when(_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                else -> MediaServerType.FLUSSONIC
            }
        }
}
