package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 25/03/2020.
 */
data class AccessRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "guestPhone") val guestPhone: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "expire") val expire: String?,
    @Json(name = "clientId") val clientId: String?
)
