package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class GetStreetsRequest(
    @Json(name = "locationId") val locationId: Int
)
