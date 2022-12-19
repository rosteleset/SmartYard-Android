package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 20/03/2020.
 */

typealias RecoveryOptionsResponse = ApiResult<List<Recovery>>?

data class Recovery(
    @Json(name = "contact")
    val contact: String = "", // 8 (987) ***-*574
    @Json(name = "id")
    val id: String = "", // 42bab6a8c274b5f3a971c1b405c75557
    @Json(name = "type")
    val type: String = "" // phone
)
