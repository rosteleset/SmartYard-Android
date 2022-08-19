package com.sesameware.domain.model.response

import com.squareup.moshi.Json
import java.io.Serializable

open class ApiResult<S> (
    @Json(name = "code") val code: Int,
    @Json(name = "name") val name: String,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: S
) : Serializable

open class ApiResultNull(
    @Json(name = "code") val code: Int,
    @Json(name = "name") val name: String,
    @Json(name = "message") val message: String
) : Serializable
