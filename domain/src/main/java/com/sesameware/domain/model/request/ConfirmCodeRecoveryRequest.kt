package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 21/03/2020.
 */
data class ConfirmCodeRecoveryRequest(
    @Json(name = "contract")
    val contract: String = "", // f70392
    @Json(name = "code")
    val code: String = "" // 1234
)
