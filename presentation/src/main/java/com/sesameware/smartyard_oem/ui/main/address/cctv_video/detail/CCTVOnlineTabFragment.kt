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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.exoplayer2.ExoPlaybackException
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.MediaServerType
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailOnlineBinding
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.*
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.adapters.DetailButtonsAdapter
import com.sesameware.smartyard_oem.ui.main.address.event_log.LinearHorizontalSpacingDecoration
import com.sesameware.smartyard_oem.ui.main.address.event_log.OnSnapPositionChangeListener
import com.sesameware.smartyard_oem.ui.main.address.event_log.SnapOnScrollListener
import com.sesameware.smartyard_oem.ui.main.address.event_log.attachSnapHelperWithListener
import timber.log.Timber

class CCTVOnlineTabFragment : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCctvDetailOnlineBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: BaseCCTVPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: ViewGroup.LayoutParams? = null

    private var canRenewToken = true

    private var currentViewHolder: CctvOnlineTabPlayerVH? = null
    private var cctvButtonsAdapter: DetailButtonsAdapter? = null
    private var cctvPlayersAdapter: CctvOnlineTabPlayerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentCctvDetailOnlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (activity as? MainActivity)?.setExitFullscreenListener(null)
        cctvButtonsAdapter = null
        cctvPlayersAdapter = null

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm __onViewCreated")
        setupCctvButtons(mCCTVViewModel.cameraList.value, mCCTVViewModel.chosenIndex.value)
        if (mCCTVViewModel.cameraList.value != null && mCCTVViewModel.chosenIndex.value != null) {
            setupCctvPlayers(mCCTVViewModel.cameraList.value!!.size, mCCTVViewModel.chosenIndex.value!!)
        }
        setupObserve()
    }

    private fun setupCctvPlayers(camerasCount: Int, selectedCamera: Int) {
        binding.cctvPlayers.apply {
            val spacing = resources.getDimensionPixelSize(R.dimen.event_log_detail_spacing)
            addItemDecoration(LinearHorizontalSpacingDecoration(spacing))
            cctvPlayersAdapter = CctvOnlineTabPlayerAdapter(::onAction, camerasCount)
            adapter = cctvPlayersAdapter
        }

        val snapHelper = PagerSnapHelper()
        val onSnapPositionChangeListener = object : OnSnapPositionChangeListener {
            override fun onSnapPositionChanged(prevPosition: Int, newPosition: Int) {
                //Timber.d("__Q__ snap position changed: prev = $prevPosition;  new = $newPosition")

                if (prevPosition != RecyclerView.NO_POSITION) {
                    mPlayer?.stop()
                    currentViewHolder?.playerView?.player = null
                }
                currentViewHolder = binding.cctvPlayers.findViewHolderForAdapterPosition(newPosition) as? CctvOnlineTabPlayerVH
                cctvButtonsAdapter!!.selectButton(newPosition)
                mCCTVViewModel.chooseCamera(newPosition)

            }
        }
        binding.cctvPlayers.attachSnapHelperWithListener(
            snapHelper,
            SnapOnScrollListener.ScrollBehavior.NOTIFY_ON_SCROLL_IDLE,
            onSnapPositionChangeListener
        )
        (binding.cctvPlayers.layoutManager as LinearLayoutManager).scrollToPosition(selectedCamera)
    }

    private fun setupCctvButtons(currentList: List<CCTVData>?, currentIndex: Int?) {
        val lm = GridLayoutManager(context, 5)
        binding.cctvButtons.layoutManager = lm
        if (currentIndex != null && currentList != null) {
            val spacingHor = resources.getDimensionPixelSize(R.dimen.cctv_buttons_hor)
            val spacingVer = resources.getDimensionPixelSize(R.dimen.cctv_buttons_ver)
            binding.cctvButtons.addItemDecoration(GridSpacingItemDecoration(5, spacingHor, spacingVer))
            cctvButtonsAdapter = DetailButtonsAdapter(
                requireContext(),
                currentIndex,
                currentList
            ) {
                // selectButton works when called from on Snap Position Changed only after manual scrolling,
                // so we need to add another call on button tap
                cctvButtonsAdapter!!.selectButton(it)
                (binding.cctvPlayers.layoutManager as LinearLayoutManager).scrollToPosition(it)
            }
            binding.cctvButtons.adapter = cctvButtonsAdapter
        }
    }

    private fun setupObserve() {
        Timber.d("debug_dmm call setupObserve")

        mCCTVViewModel.chosenCameraDistinct.observe(
            viewLifecycleOwner
        ) {
            it?.run {
                Timber.d("__Q__   releasePlayer from chosenCamera observer")
                releasePlayer()
                Timber.d("__Q__   initPlayer from chosenCamera observer")
                initPlayer(this.serverType)
                changeVideoSource(this)
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

        mCCTVViewModel.isMuted.observe(
            viewLifecycleOwner
        ) { mute ->
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                if (mute) {
                    mute()
                } else {
                    unMute()
                }
            }
        }
    }

    private fun onAction(action: CctvOnlineTabPlayerAction) {
        when (action) {
            CctvOnlineTabPlayerAction.OnFullScreenClick -> {
                mCCTVViewModel.isFullscreen.value?.let {
                    mCCTVViewModel.setFullscreen(!it)
                }
            }
            CctvOnlineTabPlayerAction.OnMuteClick -> {
                mCCTVViewModel.isMuted.value?.let {
                    mCCTVViewModel.mute(!it)
                }
            }
        }
    }

    private fun setFullscreenMode() {
        if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) return
        
        lpVideoWrap = LinearLayout.LayoutParams(binding.cctvPlayers.layoutParams as LinearLayout.LayoutParams)
        (binding.cctvPlayers.parent as ViewGroup).removeView(binding.cctvPlayers)

        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.INVISIBLE
        (activity as? MainActivity)?.binding?.llMain?.addView(binding.cctvPlayers, 0)

        (activity as? MainActivity)?.hideSystemUI()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

        val lp = binding.cctvPlayers.layoutParams as LinearLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = 0
        binding.cctvPlayers.layoutParams = lp
        binding.cctvPlayers.requestLayout()


    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setNormalMode() {
        if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) return
        
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        (binding.cctvPlayers.parent as ViewGroup).removeView(binding.cctvPlayers)
        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.VISIBLE
        binding.llVideoPlayback.addView(binding.cctvPlayers, 0)

        (activity as? MainActivity)?.showSystemUI()

        //возвращаем дефолтные layouts
        if (lpVideoWrap != null) {
            binding.cctvPlayers.layoutParams = lpVideoWrap
            binding.cctvPlayers.requestLayout()
        }

        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))
    }

    private fun unMute() {
        currentViewHolder?.let {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_volume_on_24px)
            it.mute.setImageDrawable(drawable)
        }
        mPlayer?.unMute()
    }

    private fun mute() {
        currentViewHolder?.let {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_volume_off_24px)
            it.mute.setImageDrawable(drawable)
        }
        mPlayer?.mute()
    }

    private fun createPlayer(
        serverType: MediaServerType?,
        videoView: PlayerView,
        progressView: ProgressBar,
        mute: ImageView
    ): BaseCCTVPlayer {
        Timber.d("debug_dmm createPlayer()")

        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady() {
                progressView.visibility = View.GONE
                canRenewToken = true
                (mPlayer as? DefaultCCTVPlayer)?.getPlayer()?.videoFormat?.let {
                    if (it.width > 0 && it.height > 0) {
                        (videoView.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
                    }
                }
                if (mPlayer?.playWhenReady == true) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                mute()
            }

            override fun onPlayerStateEnded() {
                progressView.visibility = View.GONE
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onPlayerStateBuffering() {
                progressView.visibility = View.VISIBLE
            }

            override fun onPlayerStateIdle() {
                progressView.visibility = View.GONE
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onPlayerError(exception: Exception) {
                progressView.visibility = View.GONE

                (exception as? ExoPlaybackException)?.let { error ->
                    if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                        if (canRenewToken) {
                            canRenewToken = false

                            //перезапрашиваем список камер
                            mCCTVViewModel.cctvModel.value?.let {
                                mCCTVViewModel.refreshCameras(it)
                            }
                        } else {
                            mCCTVViewModel.showGlobalError(error.sourceException)
                        }
                    }

                    if (error.type == ExoPlaybackException.TYPE_RENDERER) {
                        if (forceVideoTrack) {
                            forceVideoTrack = false
                            Timber.d("__Q__   releasePlayer from onPlayerError")
                            releasePlayer()
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Timber.d("__Q__   initPlayer from onPlayerError")
                            initPlayer(mCCTVViewModel.chosenCamera.value?.serverType)
                        }
                    }

                    return
                }

                mCCTVViewModel.showGlobalError(exception)
            }

            override fun onAudioAvailabilityChanged(isAvailable: Boolean) {
                mute.isVisible = isAvailable
            }
        }

        val player = when (serverType) {
            MediaServerType.MACROSCOP -> MacroscopPlayer(requireContext(), forceVideoTrack, callbacks)
            MediaServerType.FORPOST -> ForpostPlayer(requireContext(), forceVideoTrack, callbacks)
            else -> DefaultCCTVPlayer(requireContext(), forceVideoTrack, callbacks)
        }
        videoView.player = player.getPlayer()
        videoView.useController = false
        player.playWhenReady = true
        return player
    }

    fun changeVideoSource(cctvData: CCTVData) {
        currentViewHolder?.progress?.visibility = View.VISIBLE
        Timber.d("debug_dmm  prepareMedia url = ${cctvData.hls}")
        mPlayer?.prepareMedia(cctvData.hls, doPlay = true)
    }

    fun releasePlayer() {
        mPlayer?.releasePlayer()
        mPlayer = null
    }

    fun initPlayer(serverType: MediaServerType?) {
        Timber.d("debug_dmm  call initPlayer")
        val viewHolder = currentViewHolder
        if (mPlayer == null && view != null && viewHolder != null) {
            mPlayer = createPlayer(serverType, viewHolder.playerView, viewHolder.progress, viewHolder.mute)
        }
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
    ) :
        ItemDecoration() {
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

    override fun onStop() {
        super.onStop()

        Timber.d("__Q__   releasePlayer from onStop")
        releasePlayer()
        mCCTVViewModel.mute(true)
    }

    override fun onPause() {
        super.onPause()

        Timber.d("__Q__   releasePlayer from onPause")
        releasePlayer()
        mCCTVViewModel.mute(true)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        Timber.d("__Q__   initPlayer from onResume()")
        val cctvData = mCCTVViewModel.chosenCamera.value
        cctvData?.let {
            initPlayer(it.serverType)
            changeVideoSource(it)
            currentViewHolder?.mute?.isVisible = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (currentViewHolder?.playerView?.parent as? ZoomLayout)?.resetZoom()
    }
}
