package com.sesameware.domain.model.request

import com.squareup.moshi.Json

data class ListLicensePlateNumbersRequest(
    @Json(name = "flatId") val flatId: Int
)
