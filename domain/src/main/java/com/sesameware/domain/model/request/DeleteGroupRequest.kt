package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class DeleteGroupRequest(
    @param:Json(name = "groupId") val groupId: String,
    @param:Json(name = "flatId") val flatId: Int
)
