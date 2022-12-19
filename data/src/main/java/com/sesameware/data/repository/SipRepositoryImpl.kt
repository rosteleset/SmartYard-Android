package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.SipRepository
import com.sesameware.domain.model.response.SipHelpMeResponse

class SipRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : SipRepository, BaseRepository(moshi) {
    override suspend fun helpMe(): SipHelpMeResponse? {
        return safeApiCall {
            teledomApi.sipHelpMe(DataModule.BASE_URL + "sip/helpMe").getResponseBody()
        }
    }
}
