package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class AddLicensePlateNumberRequestV2(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "number") val number: String,
    @param:Json(name = "countryCode") val countryCode: String = "ru",
    @param:Json(name = "validFrom") val validFrom: String? = null,
    @param:Json(name = "validTo") val validTo: String? = null
)
