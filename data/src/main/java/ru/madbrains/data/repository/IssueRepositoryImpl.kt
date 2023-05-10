package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.IssueRepository
import ru.madbrains.domain.model.request.ActionIssueRequest
import ru.madbrains.domain.model.request.CreateIssuesRequest
import ru.madbrains.domain.model.request.DeliveryСhangeRequest
import ru.madbrains.domain.model.request.СommentRequest
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
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : IssueRepository, BaseRepository(moshi) {

    override suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse {
        return safeApiCall {
            Timber.d("__Q__ issue: $request")
            lantaApi.createIssues(request)
        }
    }

    override suspend fun listConnectIssue(): ListConnectIssueResponse {
        return safeApiCall {
            lantaApi.listConnectIssue().getResponseBody()
        }
    }

    override suspend fun actionIssue(key: String): ActionIssueResponse {
        return safeApiCall {
            lantaApi.actionIssue(ActionIssueRequest(key)).getResponseBody()
        }
    }

    override suspend fun comment(comment: String, key: String): CommentResponse {
        return safeApiCall {
            lantaApi.comment(СommentRequest(comment, key)).getResponseBody()
        }
    }

    override suspend fun deliveryChange(
        value: String,
        key: String
    ): DeliveryChangeResponse {
        return safeApiCall {
            lantaApi.deliveryChange(
                DeliveryСhangeRequest(
                    key = key,
                    customFields = listOf(
                        DeliveryСhangeRequest.CustomField(value = value)
                    )
                )
            )
                .getResponseBody()
        }
    }
}
