package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class SendNameRequest(
    @Json(name = "name") val name: String,
    @Json(name = "patronymic") val patronymic: String?,
    @Json(name = "last") val last: String?
)
