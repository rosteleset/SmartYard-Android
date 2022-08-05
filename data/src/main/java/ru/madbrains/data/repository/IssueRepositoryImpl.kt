package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.IssueRepository
import ru.madbrains.domain.model.request.ActionIssueRequest
import ru.madbrains.domain.model.request.CreateIssuesRequest
import ru.madbrains.domain.model.request.DeliveryChangeRequest
import ru.madbrains.domain.model.request.CommentRequest
import ru.madbrains.domain.model.response.ActionIssueResponse
import ru.madbrains.domain.model.response.CreateIssuesResponse
import ru.madbrains.domain.model.response.DeliveryChangeResponse
import ru.madbrains.domain.model.response.ListConnectIssueResponse
import ru.madbrains.domain.model.response.CommentResponse
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
            teledomApi.createIssues(request)
        }
    }

    override suspend fun listConnectIssue(): ListConnectIssueResponse {
        return safeApiCall {
            teledomApi.listConnectIssue().getResponseBody()
        }
    }

    override suspend fun actionIssue(key: String): ActionIssueResponse {
        return safeApiCall {
            teledomApi.actionIssue(ActionIssueRequest(key)).getResponseBody()
        }
    }

    override suspend fun comment(comment: String, key: String): CommentResponse {
        return safeApiCall {
            teledomApi.comment(CommentRequest(comment, key)).getResponseBody()
        }
    }

    override suspend fun deliveryChange(
        value: String,
        key: String
    ): DeliveryChangeResponse {
        return safeApiCall {
            teledomApi.deliveryChange(
                DeliveryChangeRequest(
                    key = key,
                    customFields = listOf(
                        DeliveryChangeRequest.CustomField(value = value)
                    )
                )
            )
                .getResponseBody()
        }
    }
}
