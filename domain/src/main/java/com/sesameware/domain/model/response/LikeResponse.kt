package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias LikeResponse = ApiResult<Like>?

data class Like(
    @param:Json(name = "faceId") val faceId: String
)
