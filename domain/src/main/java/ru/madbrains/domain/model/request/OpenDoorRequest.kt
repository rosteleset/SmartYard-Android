package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class OpenDoorRequest(
    @Json(name = "domophoneId") val domophoneId: Int,
    @Json(name = "doorId") val doorId: Int?
)
