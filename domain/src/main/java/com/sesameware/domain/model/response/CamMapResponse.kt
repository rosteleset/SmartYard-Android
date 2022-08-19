package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias CamMapResponse = ApiResult<List<CamMap>>?

data class CamMap(
    @Json(name = "id") val id: Int,
    @Json(name = "url") val url: String,
    @Json(name = "token") val token: String,
    @Json(name = "frs") val _frs: String
) {
    val frs: Boolean
        get() = _frs == "t"
}
