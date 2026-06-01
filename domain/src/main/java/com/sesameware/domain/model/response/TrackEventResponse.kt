package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias TrackEventResponse = ApiResult<TrackedEventResult>?

data class TrackedEventResult(
    @param:Json(name = "watcherId") val watcherId: Int
)
