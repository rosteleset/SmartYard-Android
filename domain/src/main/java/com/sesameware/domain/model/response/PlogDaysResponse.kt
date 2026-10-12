package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias PlogDaysResponse = ApiResult<List<PlogDays>>?

data class PlogDays(
    @param:Json(name = "day") val day: String,
    @param:Json(name = "events") val eventCount: Int
)
