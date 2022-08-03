package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.request.CreateIssuesRequest
import ru.madbrains.domain.model.response.ActionIssueResponse
import ru.madbrains.domain.model.response.CreateIssuesResponse
import ru.madbrains.domain.model.response.DeliveryChangeResponse
import ru.madbrains.domain.model.response.ListConnectIssueResponse
import ru.madbrains.domain.model.response.CommentResponse

/**
 * @author Nail Shakurov
 * Created on 01/04/2020.
 */
interface IssueRepository {
    suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse

    suspend fun listConnectIssue(): ListConnectIssueResponse

    suspend fun actionIssue(key: String): ActionIssueResponse

    suspend fun comment(comment: String, key: String): CommentResponse

    suspend fun deliveryChange(value: String, key: String): DeliveryChangeResponse
}
