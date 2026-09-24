package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class AddGroupRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "groupName") val groupName: String
)
