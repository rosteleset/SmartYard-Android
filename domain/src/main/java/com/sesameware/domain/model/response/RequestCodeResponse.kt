package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias RequestCodeResponse = ApiResult<RequestCode>?

data class RequestCode(
    @Json(name = "method") val method: String?,
    @Json(name = "confirmationNumbers") val confirmationNumbers: MutableList<String>?
)
