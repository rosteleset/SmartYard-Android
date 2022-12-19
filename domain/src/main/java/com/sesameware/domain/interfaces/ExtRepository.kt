package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.ExtListResponse
import com.sesameware.domain.model.request.ExtRequest
import com.sesameware.domain.model.response.ExtResponse

interface ExtRepository {
    suspend fun list(): ExtListResponse

    suspend fun ext(request: ExtRequest): ExtResponse
}
