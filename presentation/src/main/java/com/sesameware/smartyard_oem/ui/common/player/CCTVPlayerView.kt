package com.sesameware.smartyard_oem.ui.common.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.sesameware.smartyard_oem.databinding.ViewCctvPlayerBinding

class CCTVPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding = ViewCctvPlayerBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun loadPreview(url: String, cacheKey: String) {
        if (url.isNotBlank()) {
            Glide.with(binding.ivPreview)
                .load(url)
                .signature(ObjectKey(cacheKey))
                .into(binding.ivPreview)
            binding.ivPreview.isVisible = true
        } else {
            binding.ivPreview.isVisible = false
        }
    }

    fun showWebRtcSurface() {
        binding.playerView.isVisible = false
        binding.webRtcView.isVisible = true
        binding.webRtcView.alpha = 1f
    }

    fun showHlsSurface() {
        binding.webRtcView.isVisible = false
        binding.playerView.isVisible = true
        binding.playerView.alpha = 1f
    }

    fun showStreamReady() {
        binding.ivPreview.isVisible = false
        binding.progressBar.isVisible = false
        binding.ivNoStream.isVisible = false
    }

    fun showLoading() {
        binding.progressBar.isVisible = true
        binding.ivNoStream.isVisible = false
    }

    fun showNoStream() {
        binding.progressBar.isVisible = false
        binding.ivNoStream.isVisible = true
    }

    fun setAspectRatio(ratio: Float) {
        if (ratio > 0f) {
            binding.zoomLayout.setAspectRatio(ratio)
        }
    }

    fun resetZoom() {
        binding.zoomLayout.resetZoom()
    }

    fun setSingleTapConfirmedListener(listener: (() -> Unit)?) {
        binding.zoomLayout.setSingleTapConfirmedListener(listener)
    }

    fun setDoubleTapConfirmedListener(listener: ((Float?) -> Unit)?) {
        binding.zoomLayout.setDoubleTapConfirmedListener(listener)
    }
}
