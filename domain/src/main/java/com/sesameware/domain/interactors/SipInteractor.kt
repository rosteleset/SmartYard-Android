package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.SipRepository
import com.sesameware.domain.model.response.HelpMeData

class SipInteractor(
    private val repository: SipRepository
) {
    suspend fun helpMe(): HelpMeData? {
        return repository.helpMe()?.data
    }
}
