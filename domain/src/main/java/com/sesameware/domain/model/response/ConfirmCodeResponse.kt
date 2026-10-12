package com.sesameware.domain.model.response

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

typealias ConfirmCodeResponse = ApiResult<ConfirmCodeData>

data class ConfirmCodeData(
    @param:Json(name = "accessToken") val accessToken: String,
    @param:Json(name = "names") val names: Any? = null
)

data class UserName(
    @param:Json(name = "name")
    @SerializedName(value = "firstName", alternate = ["name"])
    val firstName: String = "",

    @param:Json(name = "patronymic")
    val patronymic: String = "",

    @param:Json(name = "last")
    @SerializedName(value = "lastName", alternate = ["last"])
    val lastName: String = ""
) {
    override fun toString(): String {
        return "$firstName $patronymic $lastName".trim()
    }
}
