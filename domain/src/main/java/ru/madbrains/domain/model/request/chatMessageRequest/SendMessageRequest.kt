package ru.madbrains.domain.model.request.chatMessageRequest

import com.squareup.moshi.Json

data class SendMessageRequest(
    @Json(name = "chat") val chatId: String,
    @Json(name = "message") val message: String?,
    @Json(name = "message_type") val messageType: String?,
    @Json(name = "images") val images: List<String>?,
)

