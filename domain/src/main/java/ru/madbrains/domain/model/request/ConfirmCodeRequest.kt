package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

data class ConfirmCodeRequest(
    @Json(name = "userPhone") val userPhone: String,
    @Json(name = "smsCode") val smsCode: String
)
