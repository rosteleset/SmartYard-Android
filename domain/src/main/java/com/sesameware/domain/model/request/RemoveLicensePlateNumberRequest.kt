package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class RemoveLicensePlateNumberRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "number") val licensePlateNumber: String
)
