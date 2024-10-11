package ru.madbrains.domain.model.request
import com.squareup.moshi.Json


data class RequestCodePushRequest(
    @Json(name = "userPhone") val userPhone: String,
    @Json(name = "type") val type: String,
    @Json(name = "pushToken") val pushToken: String
)