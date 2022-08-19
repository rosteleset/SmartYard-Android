package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.ExtRepository
import com.sesameware.domain.model.response.ExtListResponse
import com.sesameware.domain.model.request.ExtRequest
import com.sesameware.domain.model.response.ExtResponse

class ExtInteractor(
    private val repository: ExtRepository
) {
    suspend fun list(): ExtListResponse {
        return repository.list()
    }

    suspend fun ext(request: ExtRequest): ExtResponse {
        return repository.ext(request)
    }
}
