package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.content.pm.ActivityInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding

class CctvOnlineTabPlayerVH(
    private val binding: ItemCctvDetailOnlinePlayerBinding,
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var playerResizeMode: Int = 0

    val playerView get() = binding.mVideoView
    val progress get() = binding.mProgress
    val mute get() = binding.mMute
    private var isFullscreen = false

    init {
        onCreateViewHolderBind()
    }

    private fun onCreateViewHolderBind() {
        with (binding) {
            root.clipToOutline = true
            mFullScreen.setOnClickListener {
                isFullscreen = !isFullscreen
                if (isFullscreen) {
                    setFullscreenMode()
                } else {
                    setNormalMode()
                }
                onAction(CctvOnlineTabPlayerAction.OnFullScreenClick)
            }
            mMute.setOnClickListener {
                onAction(CctvOnlineTabPlayerAction.OnMuteClick)
            }
        }
    }

    private fun setFullscreenMode() {
        if ((binding.root.context as? FragmentActivity)?.requestedOrientation ==
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) return

        playerResizeMode = binding.mVideoView.resizeMode
        binding.mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_exit_fullscreen)
        binding.mFullScreen.setImageDrawable(drawable)

        binding.zlOnline.resetZoom()
    }

    private fun setNormalMode() {
        if ((binding.root.context as? FragmentActivity)?.requestedOrientation ==
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) return

        binding.mVideoView.resizeMode = playerResizeMode
        val drawable = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_cctv_enter_fullscreen)
        binding.mFullScreen.setImageDrawable(drawable)

        binding.zlOnline.resetZoom()
    }
}