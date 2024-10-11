package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class AcceptOffertaRequest(
    @Json(name = "login") val login: String,
    @Json(name = "password") val password: String
)

data class AcceptOffertaByAddressRequest(
    @Json(name = "houseId") val houseId: Int,
    @Json(name = "flat") val flat: Int
)