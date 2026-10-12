package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 31/03/2020.
 */
data class ActionIssueRequest(
    @param:Json(name = "key") val key: String,
    @param:Json(name = "action") val action: String = "Jelly.Закрыть авто"
)

data class ActionIssueRequestV2(
    @param:Json(name = "key") val key: String,
    @param:Json(name = "action") val action: String = "close",
    @param:Json(name = "deliveryType") val deliveryType: String? = null
)
