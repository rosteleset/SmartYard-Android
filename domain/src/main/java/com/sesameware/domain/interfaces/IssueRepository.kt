package com.sesameware.domain.interfaces

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
interface IssueRepository {
    suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse
    suspend fun createIssuesV2(request: CreateIssuesRequestV2): CreateIssuesResponse

    suspend fun listConnectIssue(): ListConnectIssueResponse
    suspend fun listConnectIssueV2(): ListConnectIssueResponse

    suspend fun actionIssue(key: String): ActionIssueResponse
    suspend fun actionIssueV2(request: ActionIssueRequestV2): ActionIssueResponse

    suspend fun comment(comment: String, key: String): CommentResponse
    suspend fun commentV2(comment: String, key: String): CommentResponse

    suspend fun deliveryChange(value: String, key: String): DeliveryChangeResponse
}
