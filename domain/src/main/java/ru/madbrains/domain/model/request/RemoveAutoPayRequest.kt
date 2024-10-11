package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class RemoveAutoPayRequest(
    @Json(name = "merchant")
    val merchant: String,

    @Json(name = "bindingId")
    val bindingId: String? = null,

    @Json(name = "contractTitle")
    val contractTitle: String? = null
)
