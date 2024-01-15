package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias PlogDaysResponse = ApiResult<List<PlogDays>>?

data class PlogDays(
    @Json(name = "day") val day: String,
    @Json(name = "events") val eventCount: Int
)
