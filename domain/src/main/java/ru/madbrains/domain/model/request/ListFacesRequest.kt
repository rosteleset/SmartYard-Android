package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class ListFacesRequest(
    @Json(name = "flatId") val flatId: Int
)
