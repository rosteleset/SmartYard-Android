package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class SendNameRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "patronymic") val patronymic: String?,
    @param:Json(name = "last") val last: String?
)
