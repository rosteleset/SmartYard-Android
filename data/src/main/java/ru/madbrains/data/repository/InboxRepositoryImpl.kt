package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.InboxRepository
import ru.madbrains.domain.model.request.DeliveredRequest
import ru.madbrains.domain.model.response.DeliveredResponse
import ru.madbrains.domain.model.response.InboxResponse
import ru.madbrains.domain.model.response.UnreadedResponse

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
class InboxRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : InboxRepository, BaseRepository(moshi) {

    override suspend fun inbox(): InboxResponse {
        return safeApiCall {
            teledomApi.inbox()
        }
    }

    override suspend fun unread(): UnreadedResponse {
        return safeApiCall {
            teledomApi.unread()
        }
    }

    override suspend fun delivered(messageId: String): DeliveredResponse {
        return safeApiCall {
            teledomApi.delivered(DeliveredRequest(messageId)).getResponseBody()
        }
    }
}
