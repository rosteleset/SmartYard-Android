package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class ActivateLimitRequest(
    @Json(name = "contractId")
    val contractId: Int
)
