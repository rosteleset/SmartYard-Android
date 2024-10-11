package ru.madbrains.domain.model.response

import com.squareup.moshi.Json


typealias OpenUrlResponse = ApiResult<OpenUrlItem>?

data class OpenUrlItem(
    @Json(name = "title") val title: String,
    @Json(name = "text") val text: String,
    @Json(name = "url") val url: String,
)