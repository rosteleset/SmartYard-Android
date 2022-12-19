package com.sesameware.domain.model.response

import com.squareup.moshi.Json
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

typealias CamMapResponse = ApiResult<List<CamMap>>?

data class CamMap(
    @Json(name = "id") val id: Int,
    @Json(name = "url") val url: String,
    @Json(name = "token") val token: String,
    @Json(name = "frs") val _frs: String,
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC
) {
    val frs: Boolean
        get() = _frs == "t"

    val serverType: MediaServerType
        get() {
            return when(_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                else -> MediaServerType.FLUSSONIC
            }
        }
}
