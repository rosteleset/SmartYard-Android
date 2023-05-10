package ru.madbrains.domain.model.response.chatResponse

import com.squareup.moshi.Json

data class AttachmentsItem(

    @Json(name = "id")
    val id: Int?,

    @Json(name = "message_id")
    val message_id: Int?,

    @Json(name = "file_type")
    val file_type: String?,

    @Json(name = "account_id")
    val account_id: Int?,

    @Json(name = "extension")
    val extension: String?,

    @Json(name = "data_url")
    val data_url: String?,

    @Json(name = "thumb_url")
    val thumb_url: String?,

    @Json(name = "file_size")
    val file_size: Int?,
)

