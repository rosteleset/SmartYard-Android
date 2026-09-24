package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.AttachFaceToGroupResponse
import com.sesameware.domain.model.response.ClusterFacesResponse
import com.sesameware.domain.model.response.DetachFaceFromGroupResponse
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
        flatId: Int,
        groupId: Int? = null
    ): ListFacesResponse

    suspend fun attachFaceToGroup(
        faceId: Int,
        groupId: String
    ): AttachFaceToGroupResponse

    suspend fun detachFaceFromGroup(
        faceId: Int,
        groupId: String
    ): DetachFaceFromGroupResponse

    suspend fun clusterFaces(
        flatId: Int,
        prefixName: String
    ): ClusterFacesResponse
}
