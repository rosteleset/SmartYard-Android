package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class CardsRequest(
    @Json(name = "contractName")
    val contractName: String,
)
