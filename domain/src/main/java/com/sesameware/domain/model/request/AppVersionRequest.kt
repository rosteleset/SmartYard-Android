package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 08.06.2020.
 */
data class AppVersionRequest(
    @param:Json(name = "version") val version: String,
    @param:Json(name = "platform") val platform: String
)
