package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class PayRegisterRequest(
    @Json(name = "orderNumber") val orderNumber: String,
    @Json(name = "amount") val amount: Int
)
