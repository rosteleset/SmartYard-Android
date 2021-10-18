package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.response.DeliveredResponse
import ru.madbrains.domain.model.response.InboxResponse
import ru.madbrains.domain.model.response.UnreadedResponse

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
interface InboxRepository {
    suspend fun inbox(): InboxResponse

    suspend fun unread(): UnreadedResponse

    suspend fun delivered(messageId: String): DeliveredResponse
}
