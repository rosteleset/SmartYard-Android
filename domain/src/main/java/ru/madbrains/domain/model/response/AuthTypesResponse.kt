package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias AuthTypesResponse = ApiResult<List<AuthType>>?

data class AuthType(
    @Json(name = "methodId") val methodId: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "confirmationNumbers") val confirmationNumbers: List<String>?
)
