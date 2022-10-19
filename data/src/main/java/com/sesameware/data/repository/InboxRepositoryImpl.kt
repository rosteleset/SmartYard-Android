package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.InboxRepository
import com.sesameware.domain.model.response.InboxResponse
import com.sesameware.domain.model.response.UnreadedResponse

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
            teledomApi.inbox(DataModule.BASE_URL + "inbox/inbox")
        }
    }

    override suspend fun unread(): UnreadedResponse {
        return safeApiCall {
            teledomApi.unread(DataModule.BASE_URL + "inbox/unreaded")
        }
    }
}
