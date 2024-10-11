package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class OpenUrlRequest(
    @Json(name = "houseId") val houseId: Int,
    @Json(name = "flat") val flat: Int,
    @Json(name = "domophoneId") val domophoneId: Long,
    @Json(name = "timeExpire") val timeExpire: Int = 43200,
    @Json(name = "count") val count: Int = 1,
)
