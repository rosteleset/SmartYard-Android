package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias SipHelpMeResponse = ApiResult<HelpMeData>

data class HelpMeData(
    @param:Json(name = "server") val server: String,
    @param:Json(name = "port") val port: Int,
    @param:Json(name = "transport") val transport: String,
    @param:Json(name = "extension") val extension: String,
    @param:Json(name = "pass") val pass: String,
    @param:Json(name = "dial") val dial: String,
    @param:Json(name = "stun") val stun: String?
)
