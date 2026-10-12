package com.sesameware.domain.model.request

import com.sesameware.domain.BuildConfig
import com.squareup.moshi.Json

data class RegisterPushTokenRequest(
    @param:Json(name = "pushToken") val pushToken: String,
    @param:Json(name = "pushService") val pushService: String = BuildConfig.PUSH_SERVICE,
    @param:Json(name = "platform") val platform: String = PUSH_PLATFORM,
    @param:Json(name = "bundle") val bundle: String = "default"
)

const val PUSH_PLATFORM = "android"
