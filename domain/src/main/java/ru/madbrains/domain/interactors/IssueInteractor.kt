package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.IssueRepository
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
class IssueInteractor(
    private val repository: IssueRepository
) {
    suspend fun createIssues(request: CreateIssuesRequest): CreateIssuesResponse {
        return repository.createIssues(request)
    }

    suspend fun listConnectIssue(): ListConnectIssueResponse {
        return repository.listConnectIssue()
    }

    suspend fun actionIssue(key: String): ActionIssueResponse {
        return repository.actionIssue(key)
    }

    suspend fun comment(comment: String, key: String): CommentResponse {
        return repository.comment(comment, key)
    }

    suspend fun deliveryChange(value: String, key: String): DeliveryChangeResponse {
        return repository.deliveryChange(value, key)
    }
}
