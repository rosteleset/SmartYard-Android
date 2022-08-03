package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class ExtRequest(
    @Json(name = "extId") val extId: String,
    @Json(name = "params") val params: List<Params>? = null
)

data class Params(
    @Json(name = "id") val id: String,
    @Json(name = "value") val value: String
)
