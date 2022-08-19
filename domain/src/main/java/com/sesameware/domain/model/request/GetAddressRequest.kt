package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 18/03/2020.
 */
data class GetAddressRequest(
    @Json(name = "streetId") val streetId: Int
)
