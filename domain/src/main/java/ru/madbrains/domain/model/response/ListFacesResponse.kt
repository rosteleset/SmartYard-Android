package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias ListFacesResponse = ApiResult<List<FaceData>>?

data class FaceData(
    @Json(name = "faceId") val faceId: String,
    @Json(name = "image") val faceImage: String
)
