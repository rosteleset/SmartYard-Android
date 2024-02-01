package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CctvOnlineTabPlayerVH(
    private val binding: ItemCctvDetailOnlinePlayerBinding,
    private val windowedWidth: Int,
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var previewScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE
    private var playerResizeMode: Int = 0
    private var isFullscreen: Boolean = false

    val playerView get() = binding.mVideoView
    val progress get() = binding.mProgress
    val preview get() = binding.ivPreview
    val mute get() = binding.mMute

    private val glideTarget = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            binding.ivPreview.let {
                it.setImageBitmap(resource)
                it.setFixedHeight(resource)
            }
        }
        override fun onLoadCleared(placeholder: Drawable?) {}
    }

    init {
        onCreateViewHolderBind()
    }

    private fun onCreateViewHolderBind() {
        binding.root.clipToOutline = true
        playerResizeMode = binding.mVideoView.resizeMode
        with (binding) {
            root.clipToOutline = true
            mFullScreen.setOnClickListener {
                onAction(CctvOnlineTabPlayerAction.OnFullScreenClick)
            }
            mMute.setOnClickListener {
                onAction(CctvOnlineTabPlayerAction.OnMuteClick)
            }
        }
    }

    fun bind(isFullscreen: Boolean, url: String) {
        this.isFullscreen = isFullscreen
        resetViews()
        setFullscreen()
        setPreview(url)
    }


    private fun ImageView.setFixedHeight(preview: Bitmap?) {
        if (preview == null) return
        this.post {
            val viewAspectRatio = this.width.toFloat() / this.height.toFloat()
            val bitmapAspectRatio = preview.width.toFloat() / preview.height.toFloat()
            previewScaleType = if (bitmapAspectRatio > viewAspectRatio && !isFullscreen) {
                ImageView.ScaleType.CENTER_CROP
            } else {
                ImageView.ScaleType.CENTER_INSIDE
            }
            this.scaleType = previewScaleType
        }
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
        val job = CoroutineScope(Dispatchers.IO)
        val detachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                job.cancel()
            }
        }
        binding.ivPreview.addOnAttachStateChangeListener(detachListener)
        job.launch {
            CCTVViewModel.requestForpostPreview(url)?.let { imageUrl ->
                withContext(Dispatchers.Main) {
                    Glide.with(binding.ivPreview)
                        .asBitmap()
                        .load(imageUrl)
                        .into(glideTarget)
                }
            }
        }
    }

    private fun loadPreviewAsMp4(url: String) {
        val options = RequestOptions().frame(0L)
        Glide.with(binding.ivPreview)
            .asBitmap()
            .load(url)
            .apply(options)
            .into(glideTarget)
    }

    private fun resetViews() {
        binding.mMute.isVisible = false
        binding.ivPreview.setImageBitmap(null)
    }

    fun setFullscreen() {
        if (isFullscreen) {
            setFullscreenMode()
        } else {
            setWindowedMode()
        }
    }

    private fun setFullscreenMode() {
        (binding.root.layoutParams as RecyclerView.LayoutParams).width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.root.clipToOutline = false
        binding.ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binding.mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_exit_fullscreen)
        binding.mFullScreen.setImageDrawable(drawable)
        binding.zlOnline.resetZoom()
    }

    private fun setWindowedMode() {
        binding.root.layoutParams.width = windowedWidth
        binding.root.clipToOutline = true
        binding.ivPreview.scaleType = previewScaleType
        binding.mVideoView.resizeMode = playerResizeMode
        val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_enter_fullscreen)
        binding.mFullScreen.setImageDrawable(drawable)
        binding.zlOnline.resetZoom()
    }
}