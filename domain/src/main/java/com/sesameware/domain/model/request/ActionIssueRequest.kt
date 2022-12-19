package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 31/03/2020.
 */
data class ActionIssueRequest(
    @Json(name = "key") val key: String,
    @Json(name = "action") val action: String = "Jelly.Закрыть авто"
)
