package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetAllLocationsResponse = ApiResult<List<LocationData>>

data class LocationData(
    @param:Json(name = "locationId") val locationId: Int,
    @param:Json(name = "name") val name: String
)
