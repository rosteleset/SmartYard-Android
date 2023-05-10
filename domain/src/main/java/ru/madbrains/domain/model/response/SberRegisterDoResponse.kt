package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias SberRegisterDoReponse = SberRegisterDo?

open class SberRegisterDo(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "formUrl") val formUrl: String? = null
)
