package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class CCTVRecPrepareRequest(
    @Json(name = "id") val cameraID: Int,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String
)
