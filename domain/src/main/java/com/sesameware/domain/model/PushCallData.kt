package com.sesameware.domain.model

import com.sesameware.domain.model.response.MediaServerType
import com.squareup.moshi.Json
import java.io.Serializable

data class PushCallData(
    val server: String,
    val port: String,
    val transport: PushTransport,
    val extension: String,
    var pass: String = "",
    val dtmf: String = "",
    var image: String = "",
    var live: String = "",
    val timestamp: String,
    val ttl: Int,
    val callerId: String,
    val flatId: Int,
    val flatNumber: String,
    val stun: String? = null,
    val stun_transport: String? = null,
    val hash: String? = null,
    var eyeState: Boolean = false,
    var videoType: String = "",
    var videoServer: String = "",
    var videoStream: String = "",
    var videoToken: String = "",
    var flagNotification: Boolean = false,
    @param:Json(name = "isSupport")
    private val _isSupport: String? = null,
    @param:Json(name = "title")
    private val _title: String? = null

) : Serializable {
    val isSupport: Boolean
        get() = when (_isSupport) {
            "1", "t", "true" -> true
            else -> false
        }
    val title: String
        get() = _title ?: ""

    private val mediaServerType: MediaServerType
        get() {
            return when (videoServer) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
                else -> MediaServerType.FLUSSONIC
            }
        }
    val webRtcVideoUrl: String get() =
        when (mediaServerType) {
            else -> "$videoStream/whep?token=$videoToken"
        }
}

enum class PushTransport {
    @Json(name = "udp") Udp,
    @Json(name = "tcp") Tcp,
    @Json(name = "tls") Tls
}
