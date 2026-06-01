package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class UntrackEventRequest(
    @param:Json(name = "watcherId") val watcherId: Int
)
