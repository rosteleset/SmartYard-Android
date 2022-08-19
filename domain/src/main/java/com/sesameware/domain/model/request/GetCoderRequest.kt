package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 02/04/2020.
 */
data class GetCoderRequest(
    @Json(name = "address")
    val address: String
)
