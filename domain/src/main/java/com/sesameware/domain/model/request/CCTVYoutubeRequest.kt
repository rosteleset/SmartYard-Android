package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class CCTVYoutubeRequest(
    @param:Json(name = "id") val id: Int?
)
