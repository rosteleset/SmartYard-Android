package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class CheckOffertaRequest(
    @Json(name = "login") val login: String,
    @Json(name = "password") val password: String
)

data class CheckOffertaByAddressRequest(
    @Json(name = "houseId") val houseId: Int,
    @Json(name = "flat") val flat: Int
)