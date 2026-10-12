package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class OpenDoorRequest(
    @param:Json(name = "domophoneId") val domophoneId: Int,
    @param:Json(name = "doorId") val doorId: Int?
)
