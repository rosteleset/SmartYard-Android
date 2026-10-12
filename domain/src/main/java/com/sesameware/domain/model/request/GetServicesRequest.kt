package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class GetServicesRequest(
    @param:Json(name = "houseId") val houseId: Int
)
