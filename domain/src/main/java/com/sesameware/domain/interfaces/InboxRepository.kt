package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.InboxResponse
import com.sesameware.domain.model.response.UnreadedResponse

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
interface InboxRepository {
    suspend fun inbox(): InboxResponse
    suspend fun unread(): UnreadedResponse
}
