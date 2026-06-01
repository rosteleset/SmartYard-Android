package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class GetTrackedEventsRequest(
    @param:Json(name = "flatId") val flatId: Int
)
