package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.ExtRepository
import ru.madbrains.domain.model.response.ExtListResponse
import ru.madbrains.domain.model.request.ExtRequest
import ru.madbrains.domain.model.response.ExtResponse

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
