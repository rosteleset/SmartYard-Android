package ru.madbrains.domain.model.response.chatResponse

import com.squareup.moshi.Json
import ru.madbrains.domain.model.response.ApiResult


typealias ChatWootResponse = ApiResult<List<ChatMessageResponseItem>>?

data class ChatMessageResponseItem(
    @Json(name= "content")
    val content: String?,

    @Json(name="content_attributes")
    val contentAttributes: Any?,

    @Json(name="content_type")
    val contentType: String,

    @Json(name="conversation_id")
    val conversationId: Int,

    @Json(name="attachments")
    val attachments: List<AttachmentsItem>?,

    @Json(name="created_at")
    val createdAt: Long,

    @Json(name="id")
    val id: Int,

    @Json(name="message_type")
    val messageType: Int,

    @Json(name="sender")
    val sender: Sender
)





