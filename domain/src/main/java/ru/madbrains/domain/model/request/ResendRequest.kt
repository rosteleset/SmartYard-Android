package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26/03/2020.
 */
data class ResendRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "guestPhone") val guestPhone: String
)
