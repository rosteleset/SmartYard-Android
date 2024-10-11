package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class BalanceDetailRequest(
    @Json(name = "id") val id: String,
    @Json(name = "to") val to: String,
    @Json(name = "from") val from: String
)
