package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class NewFakeRequest(
    @Json(name = "contractTitle")
    val contractTitle: String,

    @Json(name = "merchant")
    val merchant: String,

    @Json(name = "summa")
    val summa: Double,

    @Json(name = "description")
    val description: String?,

    @Json(name = "comment")
    val comment: String?,

    @Json(name = "notifyMethod")
    val notifyMethod: String?,

    @Json(name = "email")
    val email: String?,

)
