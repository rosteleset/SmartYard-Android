package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias CheckOffertaResponse = ApiResult<List<CheckOffertaItem>>?


data class CheckOffertaItem(
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String
)
