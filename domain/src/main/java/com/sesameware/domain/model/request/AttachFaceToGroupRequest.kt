package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class AttachFaceToGroupRequest(
    @param:Json(name = "faceId") val faceId: Int,
    @param:Json(name = "groupId") val groupId: String
)
