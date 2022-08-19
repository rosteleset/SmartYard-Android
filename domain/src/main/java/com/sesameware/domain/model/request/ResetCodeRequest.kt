package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 16/03/2020.
 */
data class ResetCodeRequest(
    @Json(name = "flatId") val houseId: Int
)
