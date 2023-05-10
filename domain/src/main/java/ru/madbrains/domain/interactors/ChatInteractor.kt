package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.ChatMessageRepository
import ru.madbrains.domain.model.request.chatMessageRequest.ChatMessageRequest
import ru.madbrains.domain.model.request.chatMessageRequest.SendMessageRequest
import ru.madbrains.domain.model.response.chatResponse.ChatWootResponse

class ChatInteractor(
    private val repository: ChatMessageRepository
)
{

    suspend fun getMessages(request: ChatMessageRequest): ChatWootResponse {
        return repository.getMessages(request)
    }

    suspend fun sendMessages(request: SendMessageRequest): ChatWootResponse {
        return repository.sendMessage(request)
    }

}