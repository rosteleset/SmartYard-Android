package ru.madbrains.domain.model.response.chatResponse

import com.squareup.moshi.Json

data class Sender(
    @Json(name = "additional_attributes")
    val additionalAttributes: List<String>?,

    @Json(name = "availability_status")
    val availabilityStatus: String?,

    @Json(name = "available_name")
    val availableName: String?,

    @Json(name = "avatar_url")
    val avatarUrl: String?,

    @Json(name = "custom_attributes")
    val customAttributes: List<String>?,

    @Json(name = "email")
    val email: String?,

    @Json(name = "id")
    val id: Int,

    @Json(name = "identifier")
    val identifier: String?,

    @Json(name = "name")
    val name: String?,

    @Json(name = "phone_number")
    val phoneNumber: String?,

    @Json(name = "thumbnail")
    val thumbnail: String?,

    @Json(name = "type")
    val type: String?
)
