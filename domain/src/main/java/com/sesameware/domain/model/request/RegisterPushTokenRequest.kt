package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class RegisterPushTokenRequest(
    @Json(name = "pushToken") val pushToken: String,
    @Json(name = "type") val type: Int = FCM_TYPE,
    @Json(name = "platform") val platform: String = FCM_PLATFORM
)

const val FCM_TYPE = 0
const val FCM_PLATFORM = "android"
