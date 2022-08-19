package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.SipHelpMeResponse

interface SipRepository {
    suspend fun helpMe(): SipHelpMeResponse?
}
