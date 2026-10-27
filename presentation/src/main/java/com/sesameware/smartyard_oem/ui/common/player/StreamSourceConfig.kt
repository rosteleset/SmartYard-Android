package com.sesameware.smartyard_oem.ui.common.player

import android.os.Parcelable
import com.sesameware.domain.model.response.MediaServerType
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreamSourceConfig(
    val whepUrl: String,
    val llHlsUrl: String,
    val hlsUrl: String,
    val previewUrl: String,
    val previewCacheKey: String,
    val enableSound: Boolean,
    val serverType: MediaServerType
) : Parcelable {
    val isValid: Boolean
        get() = hlsUrl.isNotBlank()
}
