package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class CctvOnlineTabPlayerViewHolder(
    private val binding: ItemCctvDetailOnlinePlayerBinding,
    private val windowedWidth: Int,
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    val playerView get() = binding.mVideoView
    val progress get() = binding.mProgress
    val mute get() = binding.mMute

    private val detachListener = object : View.OnAttachStateChangeListener {
        val jobs: MutableList<Job> = mutableListOf()

        override fun onViewAttachedToWindow(v: View) {}

        override fun onViewDetachedFromWindow(v: View) {
            jobs.forEach { it.cancel() }
        }
    }

    init {
        Timber.d("ViewHolder created at pos $bindingAdapterPosition")
        onCreateViewHolderBind()
    }

    private fun onCreateViewHolderBind() {
        with (binding) {
            ivPreview.addOnAttachStateChangeListener(detachListener)
            mFullScreen.setOnClickListener {
                onAction(CctvOnlineTabPlayerAction.OnFullScreenClick)
            }
            mMute.setOnClickListener {
                onAction(CctvOnlineTabPlayerAction.OnMuteClick)
            }
        }
    }

    fun bind(isFullscreen: Boolean, isLandscape: Boolean, url: String) {
        Timber.d("ViewHolder binded at pos $bindingAdapterPosition")
        resetViews()
        setScreenMode(isFullscreen, isLandscape)
        setPreview(url)
    }

    private fun setPreview(url: String) {
        if (url.contains(".mp4")) {
            loadPreviewAsMp4(url)
        }
        if (url.contains("/GetTranslationURL")) {
            loadPreviewAsForpost(url)
        }
    }

    private fun loadPreviewAsForpost(url: String) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            CCTVViewModel.requestForpostPreview(url)?.let { imageUrl ->
                withContext(Dispatchers.Main) {
                    Glide.with(binding.ivPreview)
                        .load(imageUrl)
                        .into(binding.ivPreview)
                }
            }
        }
        detachListener.jobs.add(job)
    }

    private fun loadPreviewAsMp4(url: String) {
        val options = RequestOptions().frame(0L)
        Glide.with(binding.ivPreview)
            .load(url)
            .apply(options)
            .into(binding.ivPreview)
    }

    private fun resetViews() {
//        binding.mMute.isVisible = false
        binding.ivPreview.setImageBitmap(null)
    }

    fun setScreenMode(isFullscreen: Boolean, isLandscape: Boolean) {
        if (isFullscreen) {
            (binding.root.layoutParams as RecyclerView.LayoutParams).width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.root.clipToOutline = false
            val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_exit_fullscreen)
            binding.mFullScreen.setImageDrawable(drawable)
        } else {
            binding.root.layoutParams.width = windowedWidth
            binding.root.clipToOutline = true
            val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_enter_fullscreen)
            binding.mFullScreen.setImageDrawable(drawable)
        }
        setScaleModes(isFullscreen, isLandscape)
        binding.zlOnline.resetZoom()
    }

    fun setScaleModes(isFullscreen: Boolean, isLandscape: Boolean) {
        if (isFullscreen && !isLandscape) {
            binding.ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        } else {
            binding.ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        if (BuildConfig.DEBUG) {
            val previewScaleType = when (binding.ivPreview.scaleType) {
                ScaleType.MATRIX -> "MATRIX"
                ScaleType.FIT_XY -> "FIT_XY"
                ScaleType.FIT_START -> "FIT_START"
                ScaleType.FIT_CENTER -> "FIT_CENTER"
                ScaleType.FIT_END -> "FIT_END"
                ScaleType.CENTER -> "CENTER"
                ScaleType.CENTER_CROP -> "CENTER_CROP"
                ScaleType.CENTER_INSIDE -> "CENTER_INSIDE"
                else -> "UNKNOWN"
            }
            val playerViewResizeMode = when (binding.mVideoView.resizeMode) {
                0 -> "RESIZE_MODE_FIT"
                1 -> "RESIZE_MODE_FIXED_WIDTH"
                2 -> "RESIZE_MODE_FIXED_HEIGHT"
                3 -> "RESIZE_MODE_FILL"
                4 -> "RESIZE_MODE_ZOOM"
                else -> "UNKNOWN"
            }
            Timber.d("ViewHolder isFullscreen=%b isLandscape=%b\npreview scale=%s player resize=%s\nat pos %d\n--------------------------",
                isFullscreen, isLandscape, previewScaleType, playerViewResizeMode, bindingAdapterPosition)
        }
    }
}