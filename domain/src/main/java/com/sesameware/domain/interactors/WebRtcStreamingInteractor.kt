package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.interfaces.WebRtcStreamingRepository
import kotlinx.coroutines.flow.Flow

class WebRtcStreamingInteractor(
    private val repository: WebRtcStreamingRepository
) {
    fun playStream(whepUrl: String): Flow<WebRtcState> {
        return repository.playStream(whepUrl)
    }
}