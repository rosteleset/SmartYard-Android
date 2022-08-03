package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
data class PutIntercomRequest(
    @Json(name = "flatId") val flatId: Int,
    @Json(name = "settings") val settings: Settings?
)

data class Settings(
    @Json(name = "enableDoorCode") val enableDoorCode: String?,
    @Json(name = "CMS") val cms: String?,
    @Json(name = "VoIP") val voip: String?,
    @Json(name = "autoOpen") val autoOpen: String?,
    @Json(name = "whiteRabbit") val whiteRabbit: Int?,
    @Json(name = "paperBill") val paperBill: String?,
    @Json(name = "disablePlog") val disablePlog: String?,
    @Json(name = "hiddenPlog") val hiddenPlog: String?,
    @Json(name = "FRSDisabled") val frsDisable: String?
)
