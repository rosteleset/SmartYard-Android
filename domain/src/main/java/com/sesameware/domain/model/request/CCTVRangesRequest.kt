package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class CCTVRangesRequest(
    @Json(name = "cameraId") val cameraId: Int
)
