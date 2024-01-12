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

data class ActionIssueRequestV2(
    @Json(name = "key") val key: String,
    @Json(name = "action") val action: String = "close",
    @Json(name = "deliveryType") val deliveryType: String? = null
)
