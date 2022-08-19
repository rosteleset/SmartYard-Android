package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.IssueRepository
import com.sesameware.domain.model.request.ActionIssueRequest
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.DeliveryChangeRequest
import com.sesameware.domain.model.request.CommentRequest
import com.sesameware.domain.model.response.ActionIssueResponse
import com.sesameware.domain.model.response.CreateIssuesResponse
import com.sesameware.domain.model.response.DeliveryChangeResponse
import com.sesameware.domain.model.response.ListConnectIssueResponse
import com.sesameware.domain.model.response.CommentResponse
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 01/04/2020.
 */
class IssueRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : IssueRepository, BaseRepository(moshi) {

    override suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse {
        return safeApiCall {
            Timber.d("__Q__ issue: $request")
            teledomApi.createIssues(
                DataModule.BASE_URL + "issues/create",
                request)
        }
    }

    override suspend fun listConnectIssue(): ListConnectIssueResponse {
        return safeApiCall {
            teledomApi.listConnectIssue(DataModule.BASE_URL + "issues/listConnect").getResponseBody()
        }
    }

    override suspend fun actionIssue(key: String): ActionIssueResponse {
        return safeApiCall {
            teledomApi.actionIssue(
                DataModule.BASE_URL + "issues/action",
                ActionIssueRequest(key)).getResponseBody()
        }
    }

    override suspend fun comment(comment: String, key: String): CommentResponse {
        return safeApiCall {
            teledomApi.comment(
                DataModule.BASE_URL + "issues/comment",
                CommentRequest(comment, key)).getResponseBody()
        }
    }

    override suspend fun deliveryChange(
        value: String,
        key: String
    ): DeliveryChangeResponse {
        return safeApiCall {
            teledomApi.deliveryChange(
                DataModule.BASE_URL + "issues/action",
                DeliveryChangeRequest(
                    key = key,
                    customFields = listOf(
                        DeliveryChangeRequest.CustomField(value = value)
                    )
                )
            ).getResponseBody()
        }
    }
}
