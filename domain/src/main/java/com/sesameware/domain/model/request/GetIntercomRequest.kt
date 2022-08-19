package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
data class GetIntercomRequest(
    @Json(name = "flatId") val houseId: Int
)
