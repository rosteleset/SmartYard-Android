package ru.madbrains.domain.model.request

import com.squareup.moshi.Json


data class PayAutoRequest(
    @Json(name = "merchant")
    val merchant: String, // "centra", "layka"

    @Json(name = "contractTitle")
    val contractTitle: String,

    @Json(name = "summa")
    val summa: Double,

    @Json(name = "bindingId")
    val bindingId: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "notifyMethod")
    val notifyMethod: String, // "push", "email"

    @Json(name = "email")
    val email: String?
)