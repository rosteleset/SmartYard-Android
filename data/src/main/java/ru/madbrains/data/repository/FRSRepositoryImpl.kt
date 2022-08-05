package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.FRSRepository
import ru.madbrains.domain.model.request.DisLikeRequest
import ru.madbrains.domain.model.request.LikeRequest
import ru.madbrains.domain.model.request.ListFacesRequest
import ru.madbrains.domain.model.response.DisLikeResponse
import ru.madbrains.domain.model.response.LikeResponse
import ru.madbrains.domain.model.response.ListFacesResponse

class FRSRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : FRSRepository, BaseRepository(moshi) {
    override suspend fun disLike(event: String?, flatId: Int?, faceId: Int?): DisLikeResponse {
        return safeApiCall {
            teledomApi.disLike(DisLikeRequest(event, flatId, faceId)).getResponseBody()
        }
    }

    override suspend fun like(event: String, comment: String): LikeResponse {
        return safeApiCall {
            teledomApi.like(LikeRequest(event, comment)).getResponseBody()
        }
    }

    override suspend fun listFaces(flatId: Int): ListFacesResponse {
        return safeApiCall {
            teledomApi.listFaces(ListFacesRequest(flatId)).getResponseBody()
        }
    }
}
