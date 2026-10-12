package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class GetStreetsRequest(
    @param:Json(name = "locationId") val locationId: Int
)
