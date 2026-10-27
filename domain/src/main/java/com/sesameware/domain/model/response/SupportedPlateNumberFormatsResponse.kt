package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias SupportedPlateNumberFormatsResponse = ApiResult<List<SupportedPlateNumberFormat>>?

data class SupportedPlateNumberFormat(
    @param:Json(name = "countryCode") val countryCode: String,
    @param:Json(name = "regex") val regex: String
)
