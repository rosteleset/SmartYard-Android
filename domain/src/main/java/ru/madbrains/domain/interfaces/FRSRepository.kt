package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.response.DisLikeResponse
import ru.madbrains.domain.model.response.LikeResponse
import ru.madbrains.domain.model.response.ListFacesResponse

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
