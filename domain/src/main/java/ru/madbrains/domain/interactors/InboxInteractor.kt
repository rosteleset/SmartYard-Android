package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.InboxRepository
import ru.madbrains.domain.model.response.DeliveredResponse
import ru.madbrains.domain.model.response.InboxResponse
import ru.madbrains.domain.model.response.UnreadedResponse

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
class InboxInteractor(
    private val repository: InboxRepository
) {

    suspend fun inbox(): InboxResponse {
        return repository.inbox()
    }

    suspend fun unread(): UnreadedResponse {
        return repository.unread()
    }

    suspend fun delivered(messageId: String): DeliveredResponse {
        return repository.delivered(messageId)
    }
}
