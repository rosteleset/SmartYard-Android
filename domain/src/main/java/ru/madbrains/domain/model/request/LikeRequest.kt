package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class LikeRequest(
    @Json(name = "event") val event: String,
    @Json(name = "comment") val comment: String
)
