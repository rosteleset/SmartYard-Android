package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ExtResponse = ApiResult<Ext>?

data class Ext(
    @Json(name = "basePath") val basePath: String? = null,
    @Json(name = "code") val code: String? = null
)
