package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
typealias InboxResponse = ApiResult<Inbox>

data class Inbox(
    @param:Json(name = "basePath")
    val basePath: String = "",
    @param:Json(name = "code")
    val code: String = "" // <!DOCTYPE html><html lang="ru"><head>..
)
