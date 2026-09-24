package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ListGroupsResponse = ApiResult<List<GroupData>>?

data class GroupData(
    @param:Json(name = "groupId") val groupId: Int,
    @param:Json(name = "groupName") val groupName: String,
    @param:Json(name = "watcherId") val watcherId: Int? = null
)
