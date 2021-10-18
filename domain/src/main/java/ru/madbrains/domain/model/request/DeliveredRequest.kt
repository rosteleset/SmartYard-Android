package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */

data class DeliveredRequest(
    @Json(name = "messageId") val messageId: String
)
