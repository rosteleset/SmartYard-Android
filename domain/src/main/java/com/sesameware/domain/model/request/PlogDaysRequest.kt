package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class PlogDaysRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "events") val events: String
)
