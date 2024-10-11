package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.response.ExtListResponse
import ru.madbrains.domain.model.request.ExtRequest
import ru.madbrains.domain.model.response.ExtOptionsResponse
import ru.madbrains.domain.model.response.ExtResponse

interface ExtRepository {
    suspend fun list(): ExtListResponse

    suspend fun ext(request: ExtRequest): ExtResponse

    suspend fun extOptions(): ExtOptionsResponse
}
