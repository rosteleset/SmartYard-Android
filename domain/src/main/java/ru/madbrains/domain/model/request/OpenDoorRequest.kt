package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class OpenDoorRequest(
    @Json(name = "domophoneId") val domophoneId: Long,
    @Json(name = "doorId") val doorId: Int?
)
