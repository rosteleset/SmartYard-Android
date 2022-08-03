package ru.madbrains.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 10.06.2020.
 */
data class UserNotificationRequest(
    @Json(name = "money")
    val money: String? = "", // t
    @Json(name = "enable")
    val enable: String? = "" // f
)
