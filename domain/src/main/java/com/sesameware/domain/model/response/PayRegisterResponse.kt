package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias PayRegisterResponse = ApiResult<PayRegister>?

open class PayRegister(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "formUrl") val formUrl: String? = null
)
