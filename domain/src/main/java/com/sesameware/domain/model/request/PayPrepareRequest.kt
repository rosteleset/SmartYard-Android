package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
data class PayPrepareRequest(
    @param:Json(name = "clientId") val clientId: String,
    @param:Json(name = "amount") val amount: String
)
