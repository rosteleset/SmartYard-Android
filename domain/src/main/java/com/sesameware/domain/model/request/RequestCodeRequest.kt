package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class RequestCodeRequest(
    @param:Json(name = "userPhone") val userPhone: String,
    @param:Json(name = "deviceToken") val deviceToken: String
)
