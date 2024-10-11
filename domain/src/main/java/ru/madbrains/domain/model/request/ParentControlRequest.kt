package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class ParentControlRequest(
    @Json(name = "clientId")
    val clientId: Int
)
