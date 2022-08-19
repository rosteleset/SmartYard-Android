package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.model.request.DisLikeRequest
import com.sesameware.domain.model.request.LikeRequest
import com.sesameware.domain.model.request.ListFacesRequest
import com.sesameware.domain.model.response.DisLikeResponse
import com.sesameware.domain.model.response.LikeResponse
import com.sesameware.domain.model.response.ListFacesResponse

class FRSRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : FRSRepository, BaseRepository(moshi) {
    override suspend fun disLike(event: String?, flatId: Int?, faceId: Int?): DisLikeResponse {
        return safeApiCall {
            teledomApi.disLike(
                DataModule.BASE_URL + "frs/disLike",
                DisLikeRequest(event, flatId, faceId)).getResponseBody()
        }
    }

    override suspend fun like(event: String, comment: String): LikeResponse {
        return safeApiCall {
            teledomApi.like(
                DataModule.BASE_URL + "frs/like",
                LikeRequest(event, comment)).getResponseBody()
        }
    }

    override suspend fun listFaces(flatId: Int): ListFacesResponse {
        return safeApiCall {
            teledomApi.listFaces(
                DataModule.BASE_URL + "frs/listFaces",
                ListFacesRequest(flatId)).getResponseBody()
        }
    }
}
