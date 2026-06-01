package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetTrackedEventsResponse = ApiResult<List<TrackedEvent>>?

data class TrackedEvent(
    @param:Json(name = "watcherId") val watcherId: Int,
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "eventType") val eventType: Int,
    @param:Json(name = "eventDetail") val eventDetail: String? = null,
    @param:Json(name = "comments") val comments: String? = null
)
