package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class CCTVRangesRequest(
    @param:Json(name = "cameraId") val cameraId: Int
)
