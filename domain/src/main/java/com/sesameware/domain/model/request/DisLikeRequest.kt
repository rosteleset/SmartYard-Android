package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class DisLikeRequest(
    @Json(name = "event") val event: String?,
    @Json(name = "flatId") val flatId: Int?,
    @Json(name = "faceId") val faceId: Int?
)
