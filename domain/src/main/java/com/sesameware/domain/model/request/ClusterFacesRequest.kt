package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ClusterFacesRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "prefixName") val prefixName: String
)
