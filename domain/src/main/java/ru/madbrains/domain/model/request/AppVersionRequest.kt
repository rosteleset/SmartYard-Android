package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 08.06.2020.
 */
data class AppVersionRequest(
    @Json(name = "version") val version: String,
    @Json(name = "platform") val platform: String
)
