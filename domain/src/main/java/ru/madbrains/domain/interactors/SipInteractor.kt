package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.SipRepository
import ru.madbrains.domain.model.response.HelpMeData

class SipInteractor(
    private val repository: SipRepository
) {
    suspend fun helpMe(): HelpMeData? {
        return repository.helpMe()?.data
    }
}
