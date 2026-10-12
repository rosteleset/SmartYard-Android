package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class CCTVRecPrepareRequest(
    @param:Json(name = "id") val cameraID: Int,
    @param:Json(name = "from") val from: String,
    @param:Json(name = "to") val to: String
)
