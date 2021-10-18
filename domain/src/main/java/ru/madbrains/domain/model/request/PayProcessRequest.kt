package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
data class PayProcessRequest(
    @Json(name = "paymentId") val paymentId: String,
    @Json(name = "sbId") val sbId: String
)
