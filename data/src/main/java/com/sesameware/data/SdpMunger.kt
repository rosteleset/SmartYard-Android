package com.sesameware.data

/**
 * Utility object for munging H.264 `profile-level-id` parameters in Session Description Protocol (SDP).
 *
 * According to RFC 6184, `profile-level-id` consists of three bytes (6 hexadecimal characters):
 * - Byte 1: `profile_idc` (identifies the base H.264 profile, e.g., 0x42 for Baseline, 0x64 for High)
 * - Byte 2: `profile-iop` (constraint flags, e.g., 0xe0 for Constrained Baseline, 0x0c for Constrained High)
 * - Byte 3: `level_idc` (indicates maximum resolution, bitrate, and frame rate, e.g., 0x1f for Level 3.1)
 *
 * Native WebRTC (libwebrtc) on Android/iOS generates constrained profile IDs by default (e.g., `640c1f`, `42e01f`).
 * However, standard IP cameras, intercoms, and streaming servers (such as Flussonic) operate with unconstrained
 * profiles (e.g., `64001f`, `42001f`) to allow passthrough streaming without transcoding.
 *
 * This utility provides bidirectional transformation:
 * 1. [mungeLocalSdp]: Modifies the outgoing SDP Offer before transmitting it to the WHEP endpoint.
 * 2. [mungeRemoteSdp]: Reverts unconstrained profiles in the incoming SDP Answer back to constrained profiles
 *    before passing it to [org.webrtc.PeerConnection.setRemoteDescription] to prevent codec mismatch errors.
 */
object SdpMunger {

    // =========================================================================
    // 1. Profile IDC (Byte 1: H.264 Base Profile in hexadecimal)
    // =========================================================================
    const val PROFILE_IDC_BASELINE  = "42" // 66 dec: Baseline Profile
    const val PROFILE_IDC_MAIN      = "4d" // 77 dec: Main Profile
    const val PROFILE_IDC_EXTENDED  = "58" // 88 dec: Extended Profile
    const val PROFILE_IDC_HIGH      = "64" // 100 dec: High Profile
    const val PROFILE_IDC_HIGH_10   = "6e" // 110 dec: High 10 Profile
    const val PROFILE_IDC_HIGH_422  = "7a" // 122 dec: High 4:2:2 Profile
    const val PROFILE_IDC_HIGH_444  = "f4" // 244 dec: High 4:4:4 Predictive Profile

    // =========================================================================
    // 2. Profile IOP / Constraints (Byte 2: constraint_set_flags in hexadecimal)
    // =========================================================================
    const val IOP_UNCONSTRAINED         = "00" // No constraints (standard CCTV/RTSP cameras)
    const val IOP_CONSTRAINED_BASELINE  = "e0" // constraint_set0..2 = 1 (WebRTC Constrained Baseline)
    const val IOP_CONSTRAINED_HIGH      = "0c" // constraint_set4..5 = 1 (WebRTC Constrained High)

    // =========================================================================
    // 3. Level IDC (Byte 3: resolution and frame rate capability limits)
    // =========================================================================
    const val LEVEL_3_0 = "1e" // Level 3.0 (up to 480p / 720p@30)
    const val LEVEL_3_1 = "1f" // Level 3.1 (720p@30 / 1080p@10) - default WebRTC level
    const val LEVEL_4_0 = "28" // Level 4.0 (1080p@30)
    const val LEVEL_4_1 = "29" // Level 4.1 (1080p@30/60)
    const val LEVEL_4_2 = "2a" // Level 4.2 (1080p@60)
    const val LEVEL_5_0 = "32" // Level 5.0 (4K@30)
    const val LEVEL_5_1 = "33" // Level 5.1 (4K@60)

    // =========================================================================
    // 4. Profile Prefixes (IDC + IOP: first 4 hexadecimal characters)
    // =========================================================================
    // Constrained profiles (advertised by native WebRTC endpoints):
    const val PREFIX_CONSTRAINED_BASELINE = "42e0"
    const val PREFIX_CONSTRAINED_MAIN     = "4de0" // or 4d40 in some legacy implementations
    const val PREFIX_CONSTRAINED_HIGH     = "640c"

    // Unconstrained profiles (streamed by RTSP cameras, intercoms, and Flussonic):
    const val PREFIX_UNCONSTRAINED_BASELINE = "4200"
    const val PREFIX_UNCONSTRAINED_MAIN     = "4d00"
    const val PREFIX_UNCONSTRAINED_HIGH     = "6400"

    // =========================================================================
    // 5. Common Full profile-level-id constants (6 hexadecimal characters)
    // =========================================================================
    const val PROFILE_LEVEL_CONSTRAINED_BASELINE_31   = "42e01f"
    const val PROFILE_LEVEL_UNCONSTRAINED_BASELINE_31 = "42001f"
    const val PROFILE_LEVEL_CONSTRAINED_HIGH_31       = "640c1f"
    const val PROFILE_LEVEL_UNCONSTRAINED_HIGH_31     = "64001f"
    const val PROFILE_LEVEL_UNCONSTRAINED_HIGH_42     = "64002a"
    const val PROFILE_LEVEL_MAIN_31                   = "4d001f"

    /**
     * Mapping for outgoing transformation: Constrained -> Unconstrained.
     */
    private val CONSTRAINED_TO_UNCONSTRAINED = listOf(
        PREFIX_CONSTRAINED_HIGH     to PREFIX_UNCONSTRAINED_HIGH,     // 640c -> 6400
        PREFIX_CONSTRAINED_BASELINE to PREFIX_UNCONSTRAINED_BASELINE, // 42e0 -> 4200
        PREFIX_CONSTRAINED_MAIN     to PREFIX_UNCONSTRAINED_MAIN      // 4de0 -> 4d00
    )

    /**
     * Mapping for incoming transformation: Unconstrained -> Constrained.
     */
    private val UNCONSTRAINED_TO_CONSTRAINED = listOf(
        PREFIX_UNCONSTRAINED_HIGH     to PREFIX_CONSTRAINED_HIGH,     // 6400 -> 640c
        PREFIX_UNCONSTRAINED_BASELINE to PREFIX_CONSTRAINED_BASELINE, // 4200 -> 42e0
        PREFIX_UNCONSTRAINED_MAIN     to PREFIX_CONSTRAINED_MAIN      // 4d00 -> 4de0
    )

    /**
     * Modifies the local SDP Offer before dispatching it to the WHEP server.
     *
     * Replaces constrained profile prefixes (`640c`, `42e0`, `4de0`) with unconstrained
     * equivalents (`6400`, `4200`, `4d00`) while preserving the original Level IDC
     * (the final two hexadecimal characters, e.g., `1f` or `28`).
     *
     * @param sdp The original local SDP offer generated by [org.webrtc.PeerConnection.createOffer].
     * @return The munged SDP string ready for HTTP POST transmission.
     */
    fun mungeLocalSdp(sdp: String): String {
        var modifiedSdp = sdp
        for ((fromPrefix, toPrefix) in CONSTRAINED_TO_UNCONSTRAINED) {
            val regex = Regex("""(?i)(profile-level-id=)$fromPrefix([0-9a-f]{2})""")
            modifiedSdp = regex.replace(modifiedSdp) { matchResult ->
                val paramName = matchResult.groupValues[1]
                val levelId = matchResult.groupValues[2]
                "$paramName$toPrefix$levelId"
            }
        }
        return modifiedSdp
    }

    /**
     * Modifies the incoming remote SDP Answer from the WHEP server before invoking
     * [org.webrtc.PeerConnection.setRemoteDescription].
     *
     * Replaces unconstrained profile prefixes (`6400`, `4200`, `4d00`) back with the
     * constrained counterparts (`640c`, `42e0`, `4de0`). This ensures libwebrtc's internal
     * codec negotiation matches the locally registered capabilities without throwing
     * profile mismatch exceptions.
     *
     * @param sdp The SDP answer received from the Flussonic WHEP endpoint.
     * @return The sanitized SDP string safe for `setRemoteDescription`.
     */
    fun mungeRemoteSdp(sdp: String): String {
        var modifiedSdp = sdp
        for ((fromPrefix, toPrefix) in UNCONSTRAINED_TO_CONSTRAINED) {
            val regex = Regex("""(?i)(profile-level-id=)$fromPrefix([0-9a-f]{2})""")
            modifiedSdp = regex.replace(modifiedSdp) { matchResult ->
                val paramName = matchResult.groupValues[1]
                val levelId = matchResult.groupValues[2]
                "$paramName$toPrefix$levelId"
            }
        }
        return modifiedSdp
    }
}