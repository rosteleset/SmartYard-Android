package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class CCTVRecDownloadRequest(
    @Json(name = "id") val fragmentID: Int
)
