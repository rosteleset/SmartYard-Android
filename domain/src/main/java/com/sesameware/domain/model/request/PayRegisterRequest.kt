package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class PayRegisterRequest(
    @param:Json(name = "orderNumber") val orderNumber: String,
    @param:Json(name = "amount") val amount: Int
)
