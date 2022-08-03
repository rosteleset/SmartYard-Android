package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class CCTVYoutubeRequest(
    @Json(name = "id") val id: Int?
)
