package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 19/03/2020.
 */

typealias OfficesResponse = ApiResult<List<Office>>

data class Office(
    @Json(name = "address")
    val address: String = "", // Октябрьская улица, 13 (ДК)
    @Json(name = "lat")
    val lat: Double = 0.0, // 52.586785
    @Json(name = "lon")
    val lon: Double = 0.0, // 41.497009
    @Json(name = "opening")
    val opening: String = "" // 09:00-19:00 (вс, пн - выходной)
)
