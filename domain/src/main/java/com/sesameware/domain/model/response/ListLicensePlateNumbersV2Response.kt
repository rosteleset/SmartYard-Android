package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ListLicensePlatesNumbersV2Response = ApiResult<List<LicensePlateNumberV2>>?

data class LicensePlateNumberV2(
    @param:Json(name = "plateNumber") val plateNumber: String,
    @param:Json(name = "countryCode") val countryCode: String? = null,
    @param:Json(name = "validFrom") val validFrom: String? = null,
    @param:Json(name = "validTo") val validTo: String? = null,
    @param:Json(name = "watcherId") val watcherId: Int? = null
)
