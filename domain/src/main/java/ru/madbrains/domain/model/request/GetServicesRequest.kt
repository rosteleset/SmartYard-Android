package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class GetServicesRequest(
    @Json(name = "houseId") val houseId: Int
)
