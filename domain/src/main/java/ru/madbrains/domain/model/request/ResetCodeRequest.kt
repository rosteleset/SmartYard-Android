package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 16/03/2020.
 */
data class ResetCodeRequest(
    @Json(name = "flatId") val houseId: Int
)

data class ResetDoorCodeRequest(
    @Json(name = "flatId") val houseId: Int,
    @Json(name = "domophoneId") val domophoneId: Long
)