package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.FRSRepository
import com.sesameware.domain.model.request.AttachFaceToGroupRequest
import com.sesameware.domain.model.request.ClusterFacesRequest
import com.sesameware.domain.model.request.DetachFaceFromGroupRequest
import com.sesameware.domain.model.request.DisLikeRequest
import com.sesameware.domain.model.request.LikeRequest
import com.sesameware.domain.model.request.ListFacesRequest
import com.sesameware.domain.model.response.AttachFaceToGroupResponse
import com.sesameware.domain.model.response.ClusterFacesResponse
import com.sesameware.domain.model.response.DetachFaceFromGroupResponse
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
                DisLikeRequest(event, flatId, faceId)
            ).getResponseBody()
        }
    }

    override suspend fun like(event: String, comment: String): LikeResponse {
        return safeApiCall {
            teledomApi.like(
                DataModule.BASE_URL + "frs/like",
                LikeRequest(event, comment)
            ).getResponseBody()
        }
    }

    override suspend fun listFaces(flatId: Int, groupId: Int?): ListFacesResponse {
        return safeApiCall {
            teledomApi.listFaces(
                DataModule.BASE_URL + "frs/listFaces",
                ListFacesRequest(flatId, groupId)
            ).getResponseBody()
        }
    }

    override suspend fun attachFaceToGroup(
        faceId: Int,
        groupId: String
    ): AttachFaceToGroupResponse {
        return safeApiCall {
            teledomApi.attachFaceToGroup(
                DataModule.BASE_URL + "frs/attachFaceToGroup",
                AttachFaceToGroupRequest(faceId, groupId)
            ).getResponseBody()
        }
    }

    override suspend fun detachFaceFromGroup(
        faceId: Int,
        groupId: String
    ): DetachFaceFromGroupResponse {
        return safeApiCall {
            teledomApi.detachFaceFromGroup(
                DataModule.BASE_URL + "frs/detachFaceFromGroup",
                DetachFaceFromGroupRequest(faceId, groupId)
            ).getResponseBody()
        }
    }

    override suspend fun clusterFaces(
        flatId: Int,
        prefixName: String
    ): ClusterFacesResponse {
        return safeApiCall {
            teledomApi.clusterFaces(
                DataModule.BASE_URL + "frs/clusterFaces",
                ClusterFacesRequest(flatId, prefixName)
            ).getResponseBody()
        }
    }
}
