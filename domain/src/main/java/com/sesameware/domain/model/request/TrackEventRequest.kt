package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class TrackEventRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "eventType") val eventType: Int,
    @param:Json(name = "eventDetail") val eventDetail: String,
    @param:Json(name = "comments") val comments: String
)
