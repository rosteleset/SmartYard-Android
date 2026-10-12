package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class LikeRequest(
    @param:Json(name = "event") val event: String,
    @param:Json(name = "comment") val comment: String
)
