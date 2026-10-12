package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetHousesResponse = ApiResult<List<HousesData>>

data class HousesData(
    @param:Json(name = "houseId") val houseId: Int,
    @param:Json(name = "number") val number: String
)
