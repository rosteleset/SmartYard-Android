package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class RequestCodeRequest(
    @Json(name = "userPhone") val userPhone: String
)
