package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class SendBalanceDetailRequest(
    @Json(name = "id") val id: Int,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "mail") val mail: String
)
