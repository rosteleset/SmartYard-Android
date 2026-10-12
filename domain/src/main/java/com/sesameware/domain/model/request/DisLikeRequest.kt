package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class DisLikeRequest(
    @param:Json(name = "event") val event: String?,
    @param:Json(name = "flatId") val flatId: Int?,
    @param:Json(name = "faceId") val faceId: Int?
)
