package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.ExtRepository
import com.sesameware.domain.model.response.ExtListResponse
import com.sesameware.domain.model.request.ExtRequest
import com.sesameware.domain.model.response.ExtResponse

class ExtRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : ExtRepository, BaseRepository(moshi) {
    override suspend fun list(): ExtListResponse {
        return safeApiCall {
            teledomApi.extList(DataModule.BASE_URL + "ext/list").getResponseBody()
        }
    }

    override suspend fun ext(request: ExtRequest): ExtResponse {
        return safeApiCall {
            teledomApi.ext(
                DataModule.BASE_URL + "ext/ext",
                request).getResponseBody()
        }
    }
}
