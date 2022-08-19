package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class CCTVAllRequest(
    @Json(name = "houseId")
    val houseId: Int
)
