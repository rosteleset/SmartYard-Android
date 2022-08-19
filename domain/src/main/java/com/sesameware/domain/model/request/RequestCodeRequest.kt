package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class RequestCodeRequest(
    @Json(name = "userPhone") val userPhone: String
)
