package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.IssueRepository
import com.sesameware.domain.model.request.ActionIssueRequestV2
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.response.ActionIssueResponse
import com.sesameware.domain.model.response.CreateIssuesResponse
import com.sesameware.domain.model.response.DeliveryChangeResponse
import com.sesameware.domain.model.response.ListConnectIssueResponse
import com.sesameware.domain.model.response.CommentResponse

/**
 * @author Nail Shakurov
 * Created on 01/04/2020.
 */
class IssueInteractor(
    private val repository: IssueRepository
) {
    suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse {
        return repository.createIssues(request)
    }

    suspend fun createIssueV2(request: CreateIssuesRequestV2): CreateIssuesResponse {
        return repository.createIssuesV2(request)
    }

    suspend fun listConnectIssue(): ListConnectIssueResponse {
        return repository.listConnectIssue()
    }

    suspend fun listConnectIssueV2(): ListConnectIssueResponse {
        return repository.listConnectIssueV2()
    }

    suspend fun actionIssue(key: String): ActionIssueResponse {
        return repository.actionIssue(key)
    }

    suspend fun actionIssueV2(request: ActionIssueRequestV2): ActionIssueResponse {
        return repository.actionIssueV2(request)
    }

    suspend fun comment(comment: String, key: String): CommentResponse {
        return repository.comment(comment, key)
    }

    suspend fun commentV2(comment: String, key: String): CommentResponse {
        return repository.commentV2(comment, key)
    }

    suspend fun deliveryChange(value: String, key: String): DeliveryChangeResponse {
        return repository.deliveryChange(value, key)
    }
}
