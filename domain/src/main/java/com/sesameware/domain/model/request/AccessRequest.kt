package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 25/03/2020.
 */
data class AccessRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "guestPhone") val guestPhone: String?,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "expire") val expire: String?,
    @param:Json(name = "clientId") val clientId: String?
)
