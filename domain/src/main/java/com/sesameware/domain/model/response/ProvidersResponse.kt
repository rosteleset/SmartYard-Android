package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ProvidersListResponse = ApiResult<List<Provider>>?

data class Provider(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "baseUrl") val baseUrl: String = ""
)
