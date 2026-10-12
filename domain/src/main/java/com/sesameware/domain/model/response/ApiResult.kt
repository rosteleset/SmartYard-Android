package com.sesameware.domain.model.response

import com.squareup.moshi.Json
import java.io.Serializable

open class ApiResult<S> (
    @param:Json(name = "code") val code: Int,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "message") val message: String,
    @param:Json(name = "data") val data: S
) : Serializable

open class ApiResultNull(
    @param:Json(name = "code") val code: Int,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "message") val message: String
) : Serializable
