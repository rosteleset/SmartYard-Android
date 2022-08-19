package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.model.response.DisLikeResponse
import com.sesameware.domain.model.response.LikeResponse
import com.sesameware.domain.model.response.ListFacesResponse

class FRSInteractor(
    private val repository: FRSRepository
) {
    suspend fun disLike(
        event: String?,
        flatId: Int?,
        faceId: Int?
    ): DisLikeResponse {
        return repository.disLike(event, flatId, faceId)
    }

    suspend fun like(
        event: String,
        comment: String
    ): LikeResponse {
        return repository.like(event, comment)
    }

    suspend fun listFaces(
        flatId: Int
    ): ListFacesResponse {
        return repository.listFaces(flatId)
    }
}
