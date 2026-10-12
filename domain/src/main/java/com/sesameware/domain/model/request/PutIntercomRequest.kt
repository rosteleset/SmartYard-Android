package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
data class PutIntercomRequest(
    @param:Json(name = "flatId") val flatId: Int,
    @param:Json(name = "settings") val settings: Settings?
)

data class Settings(
    @param:Json(name = "enableDoorCode") val enableDoorCode: String?,
    @param:Json(name = "CMS") val cms: String?,
    @param:Json(name = "VoIP") val voip: String?,
    @param:Json(name = "autoOpen") val autoOpen: String?,
    @param:Json(name = "whiteRabbit") val whiteRabbit: Int?,
    @param:Json(name = "paperBill") val paperBill: String?,
    @param:Json(name = "disablePlog") val disablePlog: String?,
    @param:Json(name = "hiddenPlog") val hiddenPlog: String?,
    @param:Json(name = "FRSDisabled") val frsDisable: String?
)
