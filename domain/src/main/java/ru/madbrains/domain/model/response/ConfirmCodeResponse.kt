package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias ConfirmCodeResponse = ApiResult<ConfirmCodeData>

data class ConfirmCodeData(
    @Json(name = "accessToken") val accessToken: String?,
    @Json(name = "names") val names: Any? = null
)

data class Name(
    @Json(name = "name") val name: String,
    @Json(name = "patronymic") var patronymic: String
)
