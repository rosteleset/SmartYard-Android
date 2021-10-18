package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.SipRepository
import ru.madbrains.domain.model.response.SipHelpMeResponse

class SipRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : SipRepository, BaseRepository(moshi) {
    override suspend fun helpMe(): SipHelpMeResponse? {
        return safeApiCall {
            lantaApi.sipHelpMe().getResponseBody()
        }
    }
}
