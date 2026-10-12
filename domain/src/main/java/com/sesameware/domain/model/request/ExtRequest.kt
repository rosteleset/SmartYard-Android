package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ExtRequest(
    @param:Json(name = "extId") val extId: String,
    @param:Json(name = "params") val params: List<Params>? = null
)

data class Params(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "value") val value: String
)
