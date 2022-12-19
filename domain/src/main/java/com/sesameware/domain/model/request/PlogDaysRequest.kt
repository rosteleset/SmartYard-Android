package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class PlogDaysRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "events") val events: String
)
