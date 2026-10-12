package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class PlogRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "day") val day: String
)
