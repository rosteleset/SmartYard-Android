package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.request.chatMessageRequest.ChatMessageRequest
import ru.madbrains.domain.model.request.chatMessageRequest.SendMessageRequest
import ru.madbrains.domain.model.response.chatResponse.ChatWootResponse

interface ChatMessageRepository {

    suspend fun getMessages(chatId: ChatMessageRequest): ChatWootResponse
    suspend fun sendMessage(request: SendMessageRequest): ChatWootResponse
}