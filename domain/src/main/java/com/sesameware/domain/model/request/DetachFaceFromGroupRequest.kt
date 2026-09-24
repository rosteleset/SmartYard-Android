package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class DetachFaceFromGroupRequest(
    @param:Json(name = "faceId") val faceId: Int,
    @param:Json(name = "groupId") val groupId: String
)
