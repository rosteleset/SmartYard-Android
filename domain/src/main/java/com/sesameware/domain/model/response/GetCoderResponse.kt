package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
typealias GetCoderResponse = ApiResult<Coder>

data class Coder(
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "address") val address: String

)
