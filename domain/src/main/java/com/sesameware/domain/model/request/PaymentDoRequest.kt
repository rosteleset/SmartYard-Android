package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
data class PaymentDoRequest(
    @Json(name = "merchant") val merchant: String,
    @Json(name = "returnUrl") val returnUrl: String,
    @Json(name = "paymentToken") val paymentToken: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "orderNumber") val orderNumber: String
)
