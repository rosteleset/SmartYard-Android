package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class MobilePayRequest(
    @Json(name = "merchant") //centra, layka
    val merchant: String,

    @Json(name = "contractTitle")
    val contractTitle: String,

    @Json(name = "summa")
    val summa: Double,

    @Json(name = "token")
    val token: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "notifyMethod")
    val notifyMethod: String, //push, email

    @Json(name = "email")
    val email: String?,

    @Json(name = "saveAuto")
    val saveAuto: Boolean?,

    @Json(name = "saveCard")
    val saveCard: Boolean?
)
