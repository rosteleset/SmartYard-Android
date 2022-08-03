package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias SipHelpMeResponse = ApiResult<HelpMeData>

data class HelpMeData(
    @Json(name = "server") val server: String,
    @Json(name = "port") val port: Int,
    @Json(name = "transport") val transport: String,
    @Json(name = "extension") val extension: String,
    @Json(name = "pass") val pass: String,
    @Json(name = "dial") val dial: String,
    @Json(name = "stun") val stun: String?
)
