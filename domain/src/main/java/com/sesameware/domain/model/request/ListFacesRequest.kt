package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ListFacesRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "groupId") val groupId: Int? = null
)
