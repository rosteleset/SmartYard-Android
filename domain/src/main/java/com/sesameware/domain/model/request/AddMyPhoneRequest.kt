package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 17/03/2020.
 */
data class AddMyPhoneRequest(
    @Json(name = "login") val login: String,
    @Json(name = "password") val password: String,
    @Json(name = "comment") val comment: String?,
    @Json(name = "notification") val notification: String?
)
