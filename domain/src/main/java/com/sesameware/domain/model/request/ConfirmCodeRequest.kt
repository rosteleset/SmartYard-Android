package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ConfirmCodeRequest(
    @param:Json(name = "userPhone") val userPhone: String,
    @param:Json(name = "smsCode") val smsCode: String,
    @param:Json(name = "deviceToken") val deviceToken: String
)
