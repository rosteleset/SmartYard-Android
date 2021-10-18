package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class PlogRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "day") val day: String
)
