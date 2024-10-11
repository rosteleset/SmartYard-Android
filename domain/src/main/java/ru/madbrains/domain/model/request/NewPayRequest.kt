package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class NewPayRequest(
    @Json(name = "merchant") //centra, layka
    val merchant: String,

    @Json(name = "contract_title")
    val contractTitle: String,

    @Json(name = "summa")
    val summa: Double,

    @Json(name = "returnUrl")
    val returnUrl: String,

    @Json(name = "saveCard")
    val saveCard: String, // "t", "f"

    @Json(name = "saveAuto")
    val saveAuto: String, // "t", "f"

    @Json(name = "notifyMethod")
    val notifyMethod: String, //  "push", "email"

    @Json(name = "email")
    val email: String?,
)