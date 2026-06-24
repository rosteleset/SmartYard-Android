package com.sesameware.data.repository

import androidx.lifecycle.Transformations.map
import com.sesameware.data.DataModule
import com.sesameware.data.executeSuspend
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import com.sesameware.domain.interfaces.MediaTrack
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.interfaces.WebRtcStreamingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.collections.forEach
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebRtcStreamingRepositoryImpl(
    private val peerConnectionFactory: PeerConnectionFactory,
    private val okHttpClient: OkHttpClient
) : WebRtcStreamingRepository {

    override fun playStream(whepUrl: String): Flow<WebRtcState> = callbackFlow {
        trySend(WebRtcState.Connecting)

        var peerConnection: PeerConnection? = null
        val pendingCandidates = mutableListOf<IceCandidate>()

        val observer = configurePeerConnectionObserver(
            onIceConnectionChange = { state ->
                if (state == PeerConnection.IceConnectionState.FAILED) {
                    trySend(WebRtcState.Error("ICE Connection Failed"))
                }
            },
            onIceCandidate = { candidate ->
                candidate?.let { pendingCandidates.add(it) }
            }
        )

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOfNotNull(
                DataModule.providerConfig.stunUrl,
                "stun:stun.l.google.com:19302"
            ).map { it.toIceServer() }
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceCandidatePoolSize = 2
        }

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)

        if (peerConnection == null) {
            trySend(WebRtcState.Error("Failed to create PeerConnection"))
            close()
            return@callbackFlow
        }

        peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
            )
        )
        peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
            )
        )

        launch(Dispatchers.IO) {
            try {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                val offer = peerConnection.createOfferSuspend(constraints)
                peerConnection.setLocalDescriptionSuspend(offer)

                withTimeoutOrNull(500) {
                    var hasSrflx = false
                    var hasHost = false

                    while (peerConnection.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE) {
                        val currentCandidates = pendingCandidates.toList()
                        hasHost = currentCandidates.any { it.sdp.contains("typ host") }
                        hasSrflx = currentCandidates.any { it.sdp.contains("typ srflx") }

                        if (hasHost && hasSrflx) {
                            break
                        }
                        delay(20)
                    }
                }

                val sdpToSend = peerConnection.localDescription.description

                val request = Request.Builder()
                    .url(whepUrl)
                    .post(sdpToSend.toRequestBody("application/sdp".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).executeSuspend()
                if (!response.isSuccessful) {
                    throw Exception("WHEP POST failed: ${response.code} ${response.message}")
                }

                val remoteSdp = response.body?.string() ?: throw Exception("Empty SDP from server")

                peerConnection.setRemoteDescriptionSuspend(
                    SessionDescription(SessionDescription.Type.ANSWER, remoteSdp)
                )

                val activeReceivers = peerConnection.receivers
                val videoTrack = activeReceivers.map { it.track() }.filterIsInstance<VideoTrack>().firstOrNull()
                val audioTrack = activeReceivers.map { it.track() }.filterIsInstance<AudioTrack>().firstOrNull()

                videoTrack?.setEnabled(false)
                audioTrack?.setEnabled(false)

                pendingCandidates.clear()

                trySend(WebRtcState.Connected(
                    videoTrack = videoTrack?.let { WebRtcMediaTrack(it) },
                    audioTrack = audioTrack?.let { WebRtcMediaTrack(it) }
                ))
            } catch (e: Exception) {
                trySend(WebRtcState.Error("WHEP Connection error: ${e.message}", e))
            }
        }

        awaitClose {
            trySend(WebRtcState.Disconnected)
            peerConnection.dispose()
        }
    }
}

private suspend fun PeerConnection.createOfferSuspend(constraints: MediaConstraints): SessionDescription =
    suspendCancellableCoroutine { cont ->
        createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) = cont.resume(sdp)
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) = cont.resumeWithException(Exception("Offer creation failed: $error"))
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

private suspend fun PeerConnection.setLocalDescriptionSuspend(sdp: SessionDescription): Unit =
    suspendCancellableCoroutine { cont ->
        setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() = cont.resume(Unit)
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String) = cont.resumeWithException(Exception("Set Local SDP failed: $error"))
        }, sdp)
    }

private suspend fun PeerConnection.setRemoteDescriptionSuspend(sdp: SessionDescription): Unit =
    suspendCancellableCoroutine { cont ->
        setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() = cont.resume(Unit)
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String) = cont.resumeWithException(Exception("Set Remote SDP failed: $error"))
        }, sdp)
    }

private fun String.toIceServer() = PeerConnection.IceServer.builder(this).createIceServer()

class WebRtcMediaTrack(val rtcTrack: MediaStreamTrack) : MediaTrack

private inline fun configurePeerConnectionObserver(
    crossinline onSignalingChange: (PeerConnection.SignalingState?) -> Unit = { _ -> },
    crossinline onIceConnectionChange: (PeerConnection.IceConnectionState?) -> Unit = { _ -> },
    crossinline onIceConnectionReceivingChange: (Boolean) -> Unit = { _ -> },
    crossinline onIceGatheringChange: (PeerConnection.IceGatheringState?) -> Unit = { _ -> },
    crossinline onIceCandidate: (IceCandidate?) -> Unit = { _ -> },
    crossinline onIceCandidatesRemoved: (Array<out IceCandidate?>?) -> Unit = { _ -> },
    crossinline onAddStream: (MediaStream?) -> Unit = { _ -> },
    crossinline onRemoveStream: (MediaStream?) -> Unit = { _ -> },
    crossinline onDataChannel: (DataChannel?) -> Unit = { _ -> },
    crossinline onRenegotiationNeeded: () -> Unit = {},
): PeerConnection.Observer =
    object : PeerConnection.Observer {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            onSignalingChange.invoke(p0)
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            onIceConnectionChange.invoke(p0)
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            onIceConnectionReceivingChange.invoke(p0)
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            onIceGatheringChange.invoke(p0)
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            onIceCandidate.invoke(p0)
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {
            onIceCandidatesRemoved.invoke(p0)
        }

        override fun onAddStream(p0: MediaStream?) {
            onAddStream.invoke(p0)
        }

        override fun onRemoveStream(p0: MediaStream?) {
            onRemoveStream.invoke(p0)
        }

        override fun onDataChannel(p0: DataChannel?) {
           onDataChannel.invoke(p0)
        }

        override fun onRenegotiationNeeded() {
            onRenegotiationNeeded.invoke()
        }
    }
