package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.ExtRepository
import ru.madbrains.domain.model.response.ExtListResponse
import ru.madbrains.domain.model.request.ExtRequest
import ru.madbrains.domain.model.response.ExtResponse

class ExtRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : ExtRepository, BaseRepository(moshi) {
    override suspend fun list(): ExtListResponse {
        return safeApiCall {
            teledomApi.extList().getResponseBody()
        }
    }

    override suspend fun ext(request: ExtRequest): ExtResponse {
        return safeApiCall {
            teledomApi.ext(request).getResponseBody()
        }
    }
}
