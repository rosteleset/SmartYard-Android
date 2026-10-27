@file:Suppress("DEPRECATION")

package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailOnlineBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.common.player.CCTVStreamPlayerManager
import com.sesameware.smartyard_oem.ui.common.player.toStreamSourceConfig
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.adapters.DetailButtonsAdapter
import com.sesameware.smartyard_oem.ui.main.player.log.PlayerLogFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import timber.log.Timber

class CCTVOnlineTabFragment : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCctvDetailOnlineBinding? = null
    private val binding get() = _binding!!

    private val playerManager: CCTVStreamPlayerManager by inject()
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: ViewGroup.LayoutParams? = null
    private var videoWrapAspectRatio = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentCctvDetailOnlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("debug_dmm __onViewCreated")
        setupAdapter(mCCTVViewModel.cameraList.value, mCCTVViewModel.chosenIndex.value)
        setupObserve()
        bindViews()
    }

    private fun bindViews() {
        binding.mFullScreen.setOnClickListener {
            mCCTVViewModel.isFullscreen.value?.let {
                mCCTVViewModel.setFullscreen(!it)
            }
        }
        binding.videoWrap.clipToOutline = true
    }

    private fun setFullscreenMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            lpVideoWrap = binding.videoWrap.layoutParams
            (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
            videoWrapAspectRatio = binding.videoWrap.aspectRatio
            binding.videoWrap.setAspectRatio(0f)

            (activity as? MainActivity)?.binding?.navHostContainer?.visibility = View.INVISIBLE
            (activity as? MainActivity)?.binding?.llMain?.addView(binding.videoWrap, 0)

            (activity as? MainActivity)?.hideSystemUI()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            binding.ivPlayerLog.isVisible = BuildConfig.DEBUG
            binding.mFullScreen.isSelected = true
            binding.videoWrap.background = null
            (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

            binding.videoWrap.applyBottomNavInsetsToPadding()
            val lp = binding.videoWrap.layoutParams as FrameLayout.LayoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            lp.topMargin = 0
            binding.videoWrap.layoutParams = lp
            binding.videoWrap.requestLayout()
            binding.cctvPlayerView.resetZoom()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setNormalMode() {
        val logFragment = childFragmentManager.findFragmentByTag(PlayerLogFragment.TAG)
        if (logFragment != null && logFragment.isAdded && !logFragment.isHidden) {
            childFragmentManager.beginTransaction().hide(logFragment).commit()
        }
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (activity as? MainActivity)?.showSystemUI()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
            if (videoWrapAspectRatio > 0f) binding.videoWrap.setAspectRatio(videoWrapAspectRatio)
            (activity as? MainActivity)?.binding?.navHostContainer?.visibility = View.VISIBLE
            binding.llVideoPlayback.addView(binding.videoWrap, 0)

            binding.ivPlayerLog.isVisible = false
            binding.mFullScreen.isSelected = false

            binding.videoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

            //возвращаем дефолтные layouts
            if (lpVideoWrap != null) {
                ViewCompat.setOnApplyWindowInsetsListener(binding.videoWrap, null)
                binding.videoWrap.layoutParams = lpVideoWrap
                binding.videoWrap.requestLayout()
            }
            binding.cctvPlayerView.resetZoom()

            (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.shaded_background))
        }
    }

    private fun setupAdapter(currentList: List<CCTVData>?, currentIndex: Int?) {
        val lm = GridLayoutManager(context, 5)
        binding.recyclerView.layoutManager = lm
        if (currentIndex != null && currentList != null) {
            val spacingHor = resources.getDimensionPixelSize(R.dimen.cctv_buttons_hor)
            val spacingVer = resources.getDimensionPixelSize(R.dimen.cctv_buttons_ver)
            binding.recyclerView.addItemDecoration(GridSpacingItemDecoration(5, spacingHor, spacingVer))
            binding.recyclerView.adapter = DetailButtonsAdapter(
                requireContext(),
                currentIndex,
                currentList
            ) {
                mCCTVViewModel.chooseCamera(it)
            }
        }
    }

    fun releasePlayer() {
        playerManager.detach()
    }

    private fun setupObserve() {
        Timber.d("debug_dmm call setupObserve")

        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) { camera ->
            camera?.let {
                playerManager.attach(
                    lifecycleOwner = viewLifecycleOwner,
                    playerView = binding.cctvPlayerView,
                    config = it.toStreamSourceConfig(),
                    muteButton = binding.mMute,
                    playerLogButton = binding.ivPlayerLog,
                    fragmentManager = childFragmentManager,
                )
                binding.ivPlayerLog.isVisible = BuildConfig.DEBUG && mCCTVViewModel.isFullscreen.value == true
            }
        }

        mCCTVViewModel.isFullscreen.observe(
            viewLifecycleOwner
        ) { fullscreen ->
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                if (fullscreen) {
                    setFullscreenMode()
                } else {
                    setNormalMode()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.cctvPlayerView.resetZoom()
    }

    companion object {
        fun newInstance() = CCTVOnlineTabFragment().apply {
            Timber.d("debug_dmm __new instance $this")
        }
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val horSpacing: Int,
        private val verSpacing: Int
    ) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            outRect.left = column * horSpacing / spanCount
            outRect.right =
                horSpacing - (column + 1) * horSpacing / spanCount
            if (position >= spanCount) {
                outRect.top = verSpacing
            }
        }
    }

    override fun onExitFullscreen() {
        if (mCCTVViewModel.isFullscreen.value == true) {
            mCCTVViewModel.setFullscreen(false)
        }
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.setExitFullscreenListener(null)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        _binding?.recyclerView?.adapter = null
        _binding = null

        super.onDestroyView()
    }
}
