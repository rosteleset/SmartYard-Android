package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias RequestCodePushResponse = ApiResult<RequestCodePushResponseData?>?


data class RequestCodePushResponseData(
    @Json(name = "requestId") val requestId: String?,
    @Json(name = "method") val type: String?,
)