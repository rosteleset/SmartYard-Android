package ru.madbrains.domain.model.request.chatMessageRequest

import com.squareup.moshi.Json

data class ChatMessageRequest(
    @Json(name = "chat") val chatId: String,
    @Json(name = "before") val before: Int? = null
)
