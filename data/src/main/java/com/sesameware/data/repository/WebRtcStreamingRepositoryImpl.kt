package com.sesameware.data.repository

import android.os.SystemClock
import com.sesameware.data.BuildConfig
import com.sesameware.data.DataModule
import com.sesameware.data.SdpMunger
import com.sesameware.data.executeSuspend
import com.sesameware.data.player.log.PlayerLogger
import com.sesameware.domain.interfaces.MediaTrack
import com.sesameware.domain.interfaces.NetworkMonitor
import com.sesameware.domain.interfaces.WebRtcState
import com.sesameware.domain.interfaces.WebRtcStreamingRepository
import com.sesameware.domain.model.NetworkStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

private class WhepHttpException(val code: Int, message: String) : Exception("HTTP $code: $message")

class WebRtcStreamingRepositoryImpl(
    private val peerConnectionFactory: PeerConnectionFactory,
    private val okHttpClient: OkHttpClient,
    private val networkMonitor: NetworkMonitor
) : WebRtcStreamingRepository {

    override fun playStream(whepUrl: String): Flow<WebRtcState> = callbackFlow {
        PlayerLogger.d("[WRTC-FLOW] Start WebRTC stream request to WHEP: $whepUrl")
        trySend(WebRtcState.Connecting)

        val diagnostics = WebRtcStartupDiagnostics()
        val readyToSendOffer = AtomicBoolean(false)
        var lastIceError: IceCandidateErrorEvent? = null

        val observer = configurePeerConnectionObserver(
            onIceCandidateError = { event ->
                lastIceError = event
                PlayerLogger.w(
                    "[WRTC-ICE] Candidate error: code=${event?.errorCode}," +
                            " text=${event?.errorText}, url=${event?.url}"
                )
            },
            onIceConnectionChange = { newState ->
                PlayerLogger.i("[WRTC-CONN] ICE Connection State: ${newState?.name}")

                val hasWiFi = networkMonitor.networkStatus.value is NetworkStatus.AvailableWiFi
                val hasCellular = networkMonitor.networkStatus.value is NetworkStatus.AvailableCellular
                val networkAvailable = hasWiFi || hasCellular
                if (newState == PeerConnection.IceConnectionState.FAILED) {
                    val error = lastIceError
                    val isNetworkIssue = error?.errorCode == 701 || !networkAvailable

                    if (isNetworkIssue) {
                        val message =
                            "${error?.errorCode ?: 0} - ${error?.errorText} ?: Network timeout"
                        PlayerLogger.w("ICE connection failed: $message")
                        trySend(WebRtcState.Fatal)
                        close()
                    } else {
                        val message =
                            "${error?.errorCode ?: -1} - ${error?.errorText} ?: ICE Failed"
                        PlayerLogger.e(message = "ICE connection failed: $message")
                        trySend(WebRtcState.Fatal)
                        close()
                    }
                } else if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                    PlayerLogger.d("[WRTC-CONN] ICE Connection Established")
                } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                    PlayerLogger.w("[WRTC-FLOW] ICE Disconnected. Shutting down connection")
                    trySend(WebRtcState.Disconnected)
                    close()
                }
            },
            onIceCandidate = { candidate ->
                candidate?.let {
                    val parts = candidate.sdp.split(" ")
                    if (parts.size < 8) {
                        PlayerLogger.i("[WRTC-ICE] Not applicable sdp: ${candidate.sdp}")
                        return@let
                    }

                    val ip = parts[4]
                    val type = parts[7]
                    val hasIpv6Host = (type == "host") && ip.contains(":")
                    val hasSfrlx = type == "srflx"
                    PlayerLogger.i("[WRTC-ICE] Candidate gathered: $type")

                    if (hasIpv6Host || hasSfrlx) {
                        readyToSendOffer.set(true)
                    }
                }
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

        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer) ?: run {
            PlayerLogger.e(message = "[WRTC-ERR] Failed to create PeerConnection")
            trySend(WebRtcState.Error)
            close()
            return@callbackFlow
        }
        PlayerLogger.i("[WRTC-CONN] PeerConnection created")

        peerConnection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        peerConnection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        val signalingJob = launch(Dispatchers.IO) {
            try {
                PlayerLogger.i("[WRTC-SIG] Creating SDP Offer...")
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                val offer = peerConnection.createOfferSuspend(constraints)
                if (!isActive) return@launch

                PlayerLogger.i("[WRTC-SIG] SDP Offer created. Setting Local Description...")
                peerConnection.setLocalDescriptionSuspend(offer)
                if (!isActive) return@launch

                PlayerLogger.i("[WRTC-SIG] Local Description set")

                val timeoutMs = 1500
                PlayerLogger.i("[WRTC-ICE] Gathering ICE candidates (Timeout: ${timeoutMs}ms)...")
                val gatheredBeforeTimeout = withTimeoutOrNull(timeoutMs.milliseconds) {
                    val isComplete = PeerConnection.IceGatheringState.COMPLETE
                    while (
                        peerConnection.iceGatheringState() != isComplete || !readyToSendOffer.get()
                    ) {
                        delay(20.milliseconds)
                    }
                    true
                }

                if (gatheredBeforeTimeout == null) {
                    PlayerLogger.w("[WRTC-ICE] ICE gathering timed out.")
                } else {
                    PlayerLogger.d("[WRTC-ICE] ICE gathering optimal condition met.")
                }

                if (!isActive) return@launch

                val constraintProfileLocalSdp = peerConnection.localDescription.description
                val unconstraintProfileLocalSdp = SdpMunger.mungeLocalSdp(constraintProfileLocalSdp)

                var unconstraintProfileRemoteSdp: String? = null
                var lastException: Exception? = null
                val maxHttpAttempts = 3
                var httpDelayMs = 500L

                for (attempt in 1..maxHttpAttempts) {
                    if (!isActive) return@launch
                    try {
                        if (attempt > 1) {
                            PlayerLogger.i("[WRTC-SIG] Retrying WHEP POST request (Attempt $attempt/$maxHttpAttempts)...")
                        } else {
                            PlayerLogger.i("[WRTC-SIG] Sending WHEP POST request to Signaling Server...")
                        }

                        val request = Request.Builder()
                            .url(whepUrl)
                            .post(
                                unconstraintProfileLocalSdp
                                    .toRequestBody("application/sdp".toMediaType())
                            )
                            .build()

                        unconstraintProfileRemoteSdp = okHttpClient.newCall(request).executeSuspend().use { response ->
                            if (!response.isSuccessful) {
                                throw WhepHttpException(response.code, response.body?.string()?.trim() ?: "empty")
                            }
                            PlayerLogger.i("[WRTC-SIG] WHEP response successful (Code: ${response.code})")

                            response.body?.string() ?: throw Exception("Empty SDP from server").also {
                                PlayerLogger.e(message = "[WRTC-ERR] Empty SDP from server")
                            }
                        }
                        break
                    } catch (e: WhepHttpException) {
                        throw e
                    } catch (e: IOException) {
                        lastException = e
                        PlayerLogger.w("[WRTC-SIG] WHEP POST request transport error (Attempt $attempt/$maxHttpAttempts): ${e.javaClass.simpleName} - ${e.message}")
                        if (attempt < maxHttpAttempts) {
                            delay(httpDelayMs.milliseconds)
                            httpDelayMs *= 2
                        }
                    }
                }

                if (unconstraintProfileRemoteSdp == null) {
                    throw lastException ?: IOException("WHEP request failed after $maxHttpAttempts attempts")
                }

                if (!isActive) return@launch
                PlayerLogger.d("[WRTC-SIG] Remote SDP received")

                val constraintProfileRemoteSdp = SdpMunger.mungeRemoteSdp(unconstraintProfileRemoteSdp)

                PlayerLogger.i("[WRTC-SIG] Setting Remote Description (Answer)...")
                peerConnection.setRemoteDescriptionSuspend(
                    SessionDescription(
                        SessionDescription.Type.ANSWER,
                        constraintProfileRemoteSdp
                    )
                )
                if (!isActive) return@launch
                PlayerLogger.i("[WRTC-SIG] Remote Description set")

                val activeReceivers = peerConnection.receivers
                val videoTrack = activeReceivers.getTrack<VideoTrack>()
                val audioTrack = activeReceivers.getTrack<AudioTrack>()

                if (videoTrack != null) PlayerLogger.d("[WRTC-MEDIA] Remote Video track extracted")
                else PlayerLogger.w("[WRTC-MEDIA] Missing Remote Video track")

                if (audioTrack != null) PlayerLogger.d("[WRTC-MEDIA] Remote Audio track extracted")
                else PlayerLogger.w("[WRTC-MEDIA] Missing Remote Audio track")

                runCatching { videoTrack?.setEnabled(false) }
                runCatching { audioTrack?.setEnabled(false) }

                PlayerLogger.d("[WRTC-FLOW] Yielding Connected: signaling ready, NOT media/renderer ready")
                val connectedAtMs = SystemClock.elapsedRealtime()
                val connectedResult = trySend(WebRtcState.Connected(
                    videoTrack = videoTrack?.let { WebRtcMediaTrack(it) },
                    audioTrack = audioTrack?.let { WebRtcMediaTrack(it) }
                ))
                if (BuildConfig.DEBUG && connectedResult.isSuccess) {
                    diagnostics.start(this@callbackFlow, peerConnection, connectedAtMs)
                }
            }  catch (e: WhepHttpException) {
                if (e.code in 400..599) {
                    val message = "[WRTC-ERR] Fatal stream error (${e.code} - ${e.message})."
                    PlayerLogger.w(message)
                    trySend(WebRtcState.Fatal)
                    close()
                } else {
                    val message = "[WHEP] Server error: ${e.message}"
                    PlayerLogger.e(message = message)
                    trySend(WebRtcState.Error)
                    close()
                }
            } catch (e: IOException) {
                val message = "[WHEP] Network/Transport error (${e.javaClass.simpleName}): ${e.message}"
                PlayerLogger.e(message = message)
                trySend(WebRtcState.Fatal)
                close()
            } catch (e: Exception) {
                if (isActive) {
                    PlayerLogger.e(e, "[WRTC-ERR] WHEP Connection Exception: ${e.message}")
                    trySend(WebRtcState.Fatal)
                    close()
                }
            }
        }

        awaitClose {
            PlayerLogger.i("[WRTC-CONN] Disconnecting and cleaning up WebRTC...")
            signalingJob.cancel()
            // Gate new native stats requests and cancel polling before freeing the native PC.
            diagnostics.stop()
            runCatching { peerConnection.close() }
            runCatching { peerConnection.dispose() }
            PlayerLogger.d("[WRTC-FLOW] WebRTC PeerConnection disposed")
        }
    }
}

/** One compact startup sample per second. Decoded frames do not imply presentation. */
private class WebRtcStartupDiagnostics {
    private val sessionId = nextSessionId.incrementAndGet()
    private val active = AtomicBoolean(true)
    private val requestLock = Any()
    private var pollingJob: Job? = null

    fun start(scope: CoroutineScope, peerConnection: PeerConnection, connectedAt: Long) {
        synchronized(requestLock) {
            if (!active.get()) return
            pollingJob = scope.launch(Dispatchers.IO) {
                val deadlineMs = connectedAt + 10_000L
                var nextSampleAtMs = connectedAt + 1_000L
                try {
                    while (isActive && active.get()) {
                        val waitMs = nextSampleAtMs - SystemClock.elapsedRealtime()
                        if (waitMs > 0) delay(waitMs)
                        if (SystemClock.elapsedRealtime() >= deadlineMs) break
                        val report = CompletableDeferred<RTCStatsReport>()
                        // Gate native access against disposal. Late callbacks only touch Java objects.
                        val connectionState = synchronized(requestLock) {
                            if (!active.get() || !isActive) return@launch
                            val state = "PC=${peerConnection.connectionState()}, ICE=${peerConnection.iceConnectionState()}"
                            peerConnection.getStats { stats ->
                                if (active.get()) report.complete(stats)
                            }
                            state
                        }
                        val remainingMs = deadlineMs - SystemClock.elapsedRealtime()
                        if (remainingMs <= 0) break
                        val snapshot = withTimeoutOrNull(minOf(1_000L, remainingMs).milliseconds) { report.await() }
                        if (!isActive || !active.get()) return@launch
                        val now = SystemClock.elapsedRealtime()
                        val prefix = "[WRTC-DIAG] session=$sessionId connected+${now - connectedAt}ms $connectionState\n"
                        if (snapshot == null) {
                            PlayerLogger.i("$prefix, stats=timeout")
                        } else if (logReport(snapshot, prefix)) {
                            return@launch
                        }
                        // Keep one-second cadence; skip missed ticks instead of emitting a burst.
                        nextSampleAtMs += 1_000L
                        if (nextSampleAtMs <= now) nextSampleAtMs = now + 1_000L
                    }
                } catch (e: Exception) {
                    // Diagnostic failures must never change stream/fallback behavior.
                    if (isActive) PlayerLogger.w("[WRTC-CONN] Startup stats stopped: ${e.message}")
                } finally {
                    active.set(false)
                }
            }
        }
    }

    fun stop() {
        synchronized(requestLock) {
            active.set(false)
            pollingJob?.cancel()
        }
    }

    private fun logReport(report: RTCStatsReport, prefix: String): Boolean {
        val stats = report.statsMap
        val videos = stats.values.filter {
            it.type == "inbound-rtp" && (it.members["kind"] == "video" || it.members["mediaType"] == "video")
        }
        val video = videos.firstOrNull()
        val transport = (video?.members?.get("transportId") as? String)?.let { stats[it] }
            ?: stats.values.firstOrNull { it.type == "transport" }
        val pair = (transport?.members?.get("selectedCandidatePairId") as? String)?.let { stats[it] }
            ?: stats.values.firstOrNull {
                it.type == "candidate-pair" && (it.members["selected"] == true || it.members["nominated"] == true)
            }
        val rttMs = (pair?.members?.get("currentRoundTripTime") as? Number)?.let { (it.toDouble() * 1_000).toLong() }
        val fields = videoFields.mapNotNull { key ->
            video?.members?.get(key)?.let { value ->
                if (key == "jitter") {
                    (value as? Number)?.let { "jitterMs=${(it.toDouble() * 1_000).toLong()}" }
                } else "$key=$value"
            }
        }.joinToString(", ")
        val decoded = videos.any { (it.members["framesDecoded"] as? Number)?.toLong()?.let { count -> count > 0 } == true }
        PlayerLogger.i("$prefix, DTLS=${transport?.members?.get("dtlsState") ?: "unknown"}," +
                " rttMs=${rttMs ?: "unknown"}, ${fields.ifEmpty { "videoStats=unavailable" }}" +
                if (decoded) ", startup=decoded (stop polling)" else "")
        return decoded
    }

    companion object {
        private val nextSessionId = AtomicLong()
        private val videoFields = listOf(
            "bytesReceived", "packetsReceived", "packetsLost", "jitter",
            "framesReceived", "framesDecoded", "keyFramesDecoded", "framesDropped", "pliCount", "nackCount"
        )
    }
}

private suspend fun PeerConnection.createOfferSuspend(constraints: MediaConstraints): SessionDescription =
    suspendCancellableCoroutine { cont ->
        createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                if (cont.isActive) cont.resume(sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {
                if (cont.isActive) cont.resumeWithException(Exception("Offer creation failed: $error"))
            }
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

private suspend fun PeerConnection.setLocalDescriptionSuspend(sdp: SessionDescription): Unit =
    suspendCancellableCoroutine { cont ->
        setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String) {
                if (cont.isActive) cont.resumeWithException(Exception("Set Local SDP failed: $error"))
            }
        }, sdp)
    }

private suspend fun PeerConnection.setRemoteDescriptionSuspend(sdp: SessionDescription): Unit =
    suspendCancellableCoroutine { cont ->
        setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String) {
                if (cont.isActive) cont.resumeWithException(Exception("Set Remote SDP failed: $error"))
            }
        }, sdp)
    }

private fun String.toIceServer() = PeerConnection.IceServer.builder(this).createIceServer()

private inline fun <reified T : MediaStreamTrack> List<RtpReceiver>.getTrack(): T? =
    firstNotNullOfOrNull { it.track() as? T }

class WebRtcMediaTrack(val rtcTrack: MediaStreamTrack) : MediaTrack

private inline fun configurePeerConnectionObserver(
    crossinline onSignalingChange: (PeerConnection.SignalingState?) -> Unit = { _ -> },
    crossinline onIceConnectionChange: (PeerConnection.IceConnectionState?) -> Unit = { _ -> },
    crossinline onConnectionChange: (PeerConnection.PeerConnectionState?) -> Unit = { _ -> },
    crossinline onIceConnectionReceivingChange: (Boolean) -> Unit = { _ -> },
    crossinline onIceGatheringChange: (PeerConnection.IceGatheringState?) -> Unit = { _ -> },
    crossinline onIceCandidate: (IceCandidate?) -> Unit = { _ -> },
    crossinline onIceCandidateError: (IceCandidateErrorEvent?) -> Unit = { _ -> },
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

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            onConnectionChange.invoke(newState)
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

        override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
            onIceCandidateError.invoke(event)
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
