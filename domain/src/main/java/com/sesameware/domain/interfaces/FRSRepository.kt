package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.DisLikeResponse
import com.sesameware.domain.model.response.LikeResponse
import com.sesameware.domain.model.response.ListFacesResponse

interface FRSRepository {
    suspend fun disLike(
        event: String?,
        flatId: Int?,
        faceId: Int?
    ): DisLikeResponse

    suspend fun like(
        event: String,
        comment: String
    ): LikeResponse

    suspend fun listFaces(
        flatId: Int
    ): ListFacesResponse
}
