package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.response.SipHelpMeResponse

interface SipRepository {
    suspend fun helpMe(): SipHelpMeResponse?
}
