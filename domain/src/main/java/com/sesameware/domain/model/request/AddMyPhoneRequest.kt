package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 17/03/2020.
 */
data class AddMyPhoneRequest(
    @param:Json(name = "login") val login: String,
    @param:Json(name = "password") val password: String,
    @param:Json(name = "comment") val comment: String?,
    @param:Json(name = "notification") val notification: String?
)
