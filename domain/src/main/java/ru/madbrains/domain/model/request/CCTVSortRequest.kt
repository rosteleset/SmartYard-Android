package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class CCTVSortRequest(
    @Json(name = "sort")
    val sort: List<Int>
)
