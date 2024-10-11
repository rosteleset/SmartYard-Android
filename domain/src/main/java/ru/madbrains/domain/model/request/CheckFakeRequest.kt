package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class CheckFakeRequest(
    @Json(name = "id")
    val id: String,

    @Json(name = "merchant")
    val merchant: String,

    @Json(name = "status")
    val status: Int,

    @Json(name = "orderId")
    val orderId: String,

    @Json(name = "processed")
    val processed: String,

    @Json(name = "test")
    val test: String
)
