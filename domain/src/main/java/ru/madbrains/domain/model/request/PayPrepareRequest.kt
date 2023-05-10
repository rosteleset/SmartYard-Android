package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
data class PayPrepareRequest(
    @Json(name = "clientId") val clientId: String,
    @Json(name = "amount") val amount: String
)
