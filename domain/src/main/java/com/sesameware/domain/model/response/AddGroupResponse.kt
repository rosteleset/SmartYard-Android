package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias AddGroupResponse = ApiResult<AddGroupResult>?

data class AddGroupResult(
    @param:Json(name = "groupId") val groupId: Int
)
