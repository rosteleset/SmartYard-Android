package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 21/03/2020.
 */
data class SentCodeRecoveryRequest(
    @Json(name = "contract")
    val contract: String = "", // f70392
    @Json(name = "contact_id")
    val contact_id: String = "" // 7b4b42e4d0727573fb542fd88eb2ef71
)
