package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class GetHousesRequest(
    @Json(name = "streetId") val streetId: Int
)
