package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.ChatMessageRepository
import ru.madbrains.domain.model.request.chatMessageRequest.ChatMessageRequest
import ru.madbrains.domain.model.request.chatMessageRequest.SendMessageRequest
import ru.madbrains.domain.model.response.chatResponse.ChatWootResponse

class ChatMessageRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : ChatMessageRepository, BaseRepository(moshi) {


    override suspend fun getMessages(chatId: ChatMessageRequest): ChatWootResponse {
        return safeApiCall {
            lantaApi.getMessages(chatId).getResponseBody()
        }
    }


    override suspend fun sendMessage(request: SendMessageRequest): ChatWootResponse {
        return safeApiCall {
            lantaApi.sendMessages(request).getResponseBody()
        }
    }
}
