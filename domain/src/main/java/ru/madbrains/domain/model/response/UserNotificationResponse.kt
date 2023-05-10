package ru.madbrains.domain.model.response

import com.google.gson.annotations.SerializedName

/**
 * @author Nail Shakurov
 * Created on 10.06.2020.
 */

typealias UserNotificationResponse = ApiResult<UserNotification>

data class UserNotification(
    @SerializedName("money")
    val money: String = "", // t
    @SerializedName("enable")
    val enable: String = "" // t
)
