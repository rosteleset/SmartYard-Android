package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 26/03/2020.
 */
data class ResendRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "guestPhone") val guestPhone: String
)
