@file:Suppress("DEPRECATION")

package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.LinearLayout
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.exoplayer2.ExoPlaybackException
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.MediaServerType
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailOnlineBinding
import com.sesameware.smartyard_oem.ui.dimenToPx
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.*
import com.sesameware.smartyard_oem.ui.main.address.event_log.OnSnapPositionChangeListener
import com.sesameware.smartyard_oem.ui.main.address.event_log.SnapOnScrollListener
import com.sesameware.smartyard_oem.ui.main.address.event_log.attachSnapHelperWithListener
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import timber.log.Timber
import kotlin.math.roundToInt


class CCTVOnlineTabFragment : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCctvDetailOnlineBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: BaseCCTVPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: ConstraintLayout.LayoutParams? = null

    private var canRenewToken = true

    private var currentViewHolder: CctvOnlineTabPlayerViewHolder? = null
    private var currentPosition: Int = RecyclerView.NO_POSITION
    private var cctvButtonsAdapter: CctvOnlineButtonsAdapter? = null
    private var cctvPlayersAdapter: CctvOnlineTabPlayerAdapter? = null
    private var cctvPlayersDecoration: CarouselDecoration? = null
    private var cctvPlayersLayoutManager: LinearLayoutManagerWithInitialPosition? = null

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
        cctvPlayersDecoration = null
        cctvPlayersLayoutManager = null

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm __onViewCreated")
        setupCctvButtons()
        setupCctvPlayers()
        setupObservers()
    }

    private fun setupCctvPlayers() {
        binding.cctvPlayers.apply {

            setHasFixedSize(true)

            val spacing = resources.getDimensionPixelSize(R.dimen.event_log_detail_spacing)
            cctvPlayersDecoration = CarouselDecoration(spacing)
            addItemDecoration(cctvPlayersDecoration!!)

            val initialCameraIndex = requireArguments().getInt(INITIAL_CAMERA_INDEX)
            cctvPlayersLayoutManager = LinearLayoutManagerWithInitialPosition(
                this@CCTVOnlineTabFragment.requireContext(),
                initialCameraIndex,
                spacing
            )
            layoutManager = cctvPlayersLayoutManager
            val previewUrls = mCCTVViewModel.cameraList.value?.map { it.preview } ?: listOf()
            cctvPlayersAdapter = CctvOnlineTabPlayerAdapter(::onAction, previewUrls)
            adapter = cctvPlayersAdapter
        }

        val snapHelper = PagerSnapHelper()
        val onSnapPositionChangeListener = object : OnSnapPositionChangeListener {
            override fun onSnapPositionChanged(prevPosition: Int, newPosition: Int) {
                //Timber.d("__Q__ snap position changed: prev = $prevPosition;  new = $newPosition")

                if (prevPosition != RecyclerView.NO_POSITION) {
                    mPlayer?.stop()
                    currentViewHolder?.playerView?.let {
                        it.alpha = 0.0f
                        it.videoSurfaceView?.isVisible = false
                        it.videoSurfaceView?.isVisible = true
                        it.player = null
                    }
                }
                currentViewHolder =
                    binding.cctvPlayers.findViewHolderForAdapterPosition(newPosition) as? CctvOnlineTabPlayerViewHolder
                currentPosition = newPosition
                val page = cctvButtonsAdapter?.selectButton(newPosition)
                if (page != null) {
                    binding.cctvButtons.smoothScrollToPosition(page)
                }
                mCCTVViewModel.chooseCamera(newPosition)
            }
        }
        binding.cctvPlayers.attachSnapHelperWithListener(
            snapHelper,
            SnapOnScrollListener.ScrollBehavior.NOTIFY_ON_SCROLL_IDLE,
            onSnapPositionChangeListener
        )
    }

    private fun setupCctvButtons() {
        binding.cctvButtons.post {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.cctvButtons)

            val buttonWidth = resources.getDimensionPixelSize(R.dimen.cctv_detail_button_size)
            val pageWidth = (binding.cctvButtons.width * CctvOnlineButtonsAdapter.ITEM_TO_PARENT_WIDTH_RATIO).toInt()
            val spacing = (pageWidth - buttonWidth * BUTTONS_PER_ROW) / (BUTTONS_PER_ROW - 1)
            val decoration = CarouselDecoration(spacing)
            binding.cctvButtons.addItemDecoration(decoration)

            val firstRowHeight = requireContext().dimenToPx(R.dimen.cctv_detail_button_size)
            val otherRowHeight = requireContext().dimenToPx(R.dimen.cctv_buttons_ver) + firstRowHeight
            val availableHeight = (binding.root.height -
                    binding.root.paddingBottom -
                    binding.cctvPlayers.height -
                    binding.cctvPlayers.marginTop -
                    binding.cctvButtons.marginTop)
            val rows = if (availableHeight > firstRowHeight) {
                (availableHeight - firstRowHeight) / otherRowHeight + 1
            } else {
                1
            }
            cctvButtonsAdapter = CctvOnlineButtonsAdapter(
                rows,
                BUTTONS_PER_ROW,
                mCCTVViewModel.cameraList.value!!.size,
                mCCTVViewModel.chosenIndex.value!!
            ) {
                cctvPlayersLayoutManager?.scrollToPositionWithOffset(it, cctvPlayersLayoutManager!!.posOffset)
            }
            binding.cctvButtons.adapter = cctvButtonsAdapter
        }
    }

    private fun setupObservers() {
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
                    setWindowedMode()
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

        lpVideoWrap = binding.cctvPlayers.layoutParams as ConstraintLayout.LayoutParams
        binding.root.removeView(binding.cctvPlayers)

        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.INVISIBLE
        (activity as? MainActivity)?.binding?.llMain?.addView(binding.cctvPlayers, 0)
        binding.cctvPlayers.removeItemDecoration(cctvPlayersDecoration!!)

        (activity as? MainActivity)?.hideSystemUI()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

        val lp = binding.cctvPlayers.layoutParams as LinearLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = 0
        binding.cctvPlayers.layoutParams = lp
        binding.cctvPlayers.requestLayout()

        binding.cctvButtons.isVisible = false

        cctvPlayersAdapter?.setFullscreen(true, currentPosition)
        currentViewHolder?.setFullscreen(true)
        currentViewHolder?.playerView?.post {
            binding.cctvPlayers.scrollToPosition(currentPosition)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setWindowedMode() {
        if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) return
        
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        (binding.cctvPlayers.parent as ViewGroup).removeView(binding.cctvPlayers)
        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.VISIBLE
        binding.root.addView(binding.cctvPlayers, 0)
        binding.cctvPlayers.addItemDecoration(cctvPlayersDecoration!!)

        (activity as? MainActivity)?.showSystemUI()

        //возвращаем дефолтные layouts
        if (lpVideoWrap != null) {
            binding.cctvPlayers.layoutParams = lpVideoWrap
            binding.root.requestLayout()
        }

        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))

        binding.cctvButtons.isVisible = true

        cctvPlayersAdapter?.setFullscreen(false, currentPosition)
        currentViewHolder?.setFullscreen(false)

        cctvPlayersLayoutManager?.scrollToPositionWithOffset(currentPosition, cctvPlayersLayoutManager!!.posOffset)
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
        viewHolder: CctvOnlineTabPlayerViewHolder
    ): BaseCCTVPlayer {
        Timber.d("debug_dmm createPlayer()")

        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady() {
                viewHolder.progress.visibility = View.GONE
                viewHolder.playerView.alpha = 1.0f
                canRenewToken = true
                mPlayer?.getPlayer()?.videoFormat?.let {
                    if (it.width > 0 && it.height > 0) {
                        (viewHolder.playerView.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
                    }
                }
                if (mPlayer?.playWhenReady == true) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                mute()
            }

            override fun onPlayerStateEnded() {
                viewHolder.progress.visibility = View.GONE
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onPlayerStateBuffering() {
                viewHolder.progress.visibility = View.VISIBLE
                viewHolder.playerView.alpha = 0.0f
            }

            override fun onPlayerStateIdle() {
                viewHolder.progress.visibility = View.GONE
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onPlayerError(exception: Exception) {
                viewHolder.progress.visibility = View.GONE

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
                viewHolder.mute.isVisible = isAvailable
            }
        }

        val player = when (serverType) {
            MediaServerType.MACROSCOP -> MacroscopPlayer(requireContext(), forceVideoTrack, callbacks)
            MediaServerType.FORPOST -> ForpostPlayer(requireContext(), forceVideoTrack, callbacks)
            else -> DefaultCCTVPlayer(requireContext(), forceVideoTrack, callbacks)
        }
        viewHolder.playerView.player = player.getPlayer()
        viewHolder.playerView.useController = false
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
            mPlayer = createPlayer(serverType, viewHolder)
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

    companion object {
        private const val BUTTONS_PER_ROW = 5
        private const val INITIAL_CAMERA_INDEX = "InitialCameraIndex"
        fun newInstance(initialCameraIndex: Int) = CCTVOnlineTabFragment().apply {
            arguments = Bundle().apply {
                putInt(INITIAL_CAMERA_INDEX, initialCameraIndex)
            }
            Timber.d("debug_dmm __new instance $this")
        }
    }
}

class CarouselDecoration(@Px private val innerSpacing: Int) : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemPosition = parent.getChildAdapterPosition(view)
        val halfInnerSpacing = innerSpacing / 2
        val outerSpacing = (parent.measuredWidth - view.layoutParams.width) / 2
        outRect.left = if (itemPosition > 0) halfInnerSpacing else outerSpacing
        outRect.right = if (itemPosition < state.itemCount - 1) halfInnerSpacing else outerSpacing
    }
}

class LinearLayoutManagerWithInitialPosition(
    context: Context,
    private var initialPos: Int,
    private val itemDecorationSpacing: Int
) : LinearLayoutManager(context,RecyclerView.HORIZONTAL, false) {
    var posOffset: Int = -1
        private set

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val parentWidth = width
        if (initialPos > -1 && state.itemCount > 0) {
            posOffset =
                (((1 - CctvOnlineTabPlayerAdapter.ITEM_TO_PARENT_WIDTH_RATIO) * parentWidth - itemDecorationSpacing) / 2)
                    .roundToInt()
            scrollToPositionWithOffset(initialPos, posOffset)
            initialPos = -1
        }
        super.onLayoutChildren(recycler, state)
    }
}
