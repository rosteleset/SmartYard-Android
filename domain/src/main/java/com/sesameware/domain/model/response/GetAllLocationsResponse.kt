package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetAllLocationsResponse = ApiResult<List<LocationData>>

data class LocationData(
    @Json(name = "locationId") val locationId: Int,
    @Json(name = "name") val name: String
)
