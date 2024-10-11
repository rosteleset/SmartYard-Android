package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class AutoPayRequest(
    @Json(name = "merchant") // "centra", "layka"
    val merchant: String,

    @Json(name = "bindingId")
    val bindingId: String? = null,

    @Json(name = "contractTitle")
    val contractTitle: String? = null
)
