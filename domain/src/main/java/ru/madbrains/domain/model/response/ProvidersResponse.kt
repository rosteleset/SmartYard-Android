package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias ProvidersListResponse = ApiResult<List<Provider>>?

data class Provider(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "baseUrl") val baseUrl: String = ""
)
