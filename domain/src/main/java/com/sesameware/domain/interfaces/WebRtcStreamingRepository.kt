package com.sesameware.domain.interfaces

import kotlinx.coroutines.flow.Flow

interface MediaTrack

interface WebRtcStreamingRepository {
    fun playStream(whepUrl: String): Flow<WebRtcState>
}

sealed interface WebRtcState {
    data object Idle : WebRtcState
    data object Connecting : WebRtcState
    data class Connected(val videoTrack: MediaTrack?, val audioTrack: MediaTrack?) : WebRtcState
    data object Error : WebRtcState
    data object Fatal : WebRtcState
    data object Disconnected : WebRtcState
}