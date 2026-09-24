package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ListFacesResponse = ApiResult<List<FaceData>>?

data class FaceData(
    @param:Json(name = "faceId") val faceId: String,
    @param:Json(name = "image") val faceImage: String,
    @param:Json(name = "groupId") val groupId: Int?
)
