package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 24/03/2020.
 */
data class QRRequest(
    @Json(name = "QR") val QR: String
)
