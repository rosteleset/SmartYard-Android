package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class GetServicesRequest(
    @Json(name = "houseId") val houseId: Int
)
