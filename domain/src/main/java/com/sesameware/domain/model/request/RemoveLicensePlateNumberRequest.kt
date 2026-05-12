package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class RemoveLicensePlateNumberRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "number") val licensePlateNumber: String
)
