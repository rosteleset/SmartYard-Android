package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
typealias UnreadedResponse = ApiResult<Unreaded>

data class Unreaded(
    @Json(name = "count")
    val count: Int = 0, // 0
    @Json(name = "chat")
    val chat: Int = 0 // 0
)
