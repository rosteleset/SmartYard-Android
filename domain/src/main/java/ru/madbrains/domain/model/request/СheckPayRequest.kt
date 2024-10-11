package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class СheckPayRequest(
    @Json(name = "mdOrder")
    val mdOrder: String?,

    @Json(name = "orderId")
    val orderId: String?
)
