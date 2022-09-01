package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
typealias InboxResponse = ApiResult<Inbox>

data class Inbox(
    @Json(name = "basePath")
    val basePath: String = "",
    @Json(name = "code")
    val code: String = "" // <!DOCTYPE html><html lang="ru"><head>..
)
