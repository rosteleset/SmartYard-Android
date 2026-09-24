package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.model.response.AttachFaceToGroupResponse
import com.sesameware.domain.model.response.ClusterFacesResponse
import com.sesameware.domain.model.response.DetachFaceFromGroupResponse
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
        flatId: Int,
        groupId: Int? = null
    ): ListFacesResponse {
        return repository.listFaces(flatId, groupId)
    }

    suspend fun attachFaceToGroup(
        faceId: Int,
        groupId: String
    ): AttachFaceToGroupResponse {
        return repository.attachFaceToGroup(faceId, groupId)
    }

    suspend fun detachFaceFromGroup(
        faceId: Int,
        groupId: String
    ): DetachFaceFromGroupResponse {
        return repository.detachFaceFromGroup(faceId, groupId)
    }

    suspend fun clusterFaces(
        flatId: Int,
        prefixName: String
    ): ClusterFacesResponse {
        return repository.clusterFaces(flatId, prefixName)
    }
}
