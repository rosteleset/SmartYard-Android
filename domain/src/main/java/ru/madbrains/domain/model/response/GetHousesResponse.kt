package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias GetHousesResponse = ApiResult<List<HousesData>>

data class HousesData(
    @Json(name = "houseId") val houseId: Int,
    @Json(name = "number") val number: String
)
