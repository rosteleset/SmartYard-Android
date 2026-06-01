package com.sesameware.smartyard_oem.ui

import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.VideoCodecInfo
import org.webrtc.VideoDecoder
import org.webrtc.VideoDecoderFactory

/**
 * Декоратор, который отключает декларирование и обработку H265 в WebRTC,
 * предотвращая нативные краши на этапе согласования кодеков.
 */
class SafeVideoDecoderFactory(
    eglContext: org.webrtc.EglBase.Context?,
    private val disableHighProfile: Boolean = false
) : VideoDecoderFactory {
    private val delegate = DefaultVideoDecoderFactory(eglContext)

    override fun createDecoder(codecInfo: VideoCodecInfo?): VideoDecoder? {
        val name = codecInfo?.name.orEmpty()
        if (name.equals("H265", ignoreCase = true) || name.equals("HEVC", ignoreCase = true)) {
            return null
        }
        return delegate.createDecoder(codecInfo)
    }

    override fun getSupportedCodecs(): Array<VideoCodecInfo> {
        return delegate.supportedCodecs.filter { codec ->
            val name = codec.name.orEmpty()

            if (name.equals("H265", ignoreCase = true) || name.equals("HEVC", ignoreCase = true)) {
                return@filter false
            }

            if (disableHighProfile && name.equals("H264", ignoreCase = true)) {
                val profileLevelId = codec.params["profile-level-id"]
                // "42e01f" - Baseline Profile. "64001f" etc. - High Profile.
                if (profileLevelId != null && !profileLevelId.startsWith("42")) {
                    return@filter false
                }
            }

            true
        }.toTypedArray()
    }
}