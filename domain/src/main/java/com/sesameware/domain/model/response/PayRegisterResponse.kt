package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias PayRegisterResponse = ApiResult<PayRegister>?

open class PayRegister(
    @param:Json(name = "orderId") val orderId: String? = null,
    @param:Json(name = "formUrl") val formUrl: String? = null
)
