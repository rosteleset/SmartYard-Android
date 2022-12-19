package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ListFacesRequest(
    @Json(name = "flatId") val flatId: Int
)
