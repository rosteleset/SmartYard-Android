package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.ExtRepository
import ru.madbrains.domain.model.response.ExtListResponse
import ru.madbrains.domain.model.request.ExtRequest
import ru.madbrains.domain.model.response.ExtOptionsResponse
import ru.madbrains.domain.model.response.ExtResponse

class ExtRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : ExtRepository, BaseRepository(moshi) {
    override suspend fun list(): ExtListResponse {
        return safeApiCall {
            lantaApi.extList().getResponseBody()
        }
    }

    override suspend fun ext(request: ExtRequest): ExtResponse {
        return safeApiCall {
            lantaApi.ext(request).getResponseBody()
        }
    }


    override suspend fun extOptions(): ExtOptionsResponse {
        return safeApiCall {
            lantaApi.extOptions().getResponseBody()
        }
    }
}
