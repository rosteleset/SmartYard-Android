package ru.madbrains.smartyard.ui.main.address.cctv_video.detail

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_cctv_detail_online.*
import kotlinx.android.synthetic.main.fragment_cctv_detail_online.videoWrap
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.ExitFullscreenListener
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.ZoomLayout
import ru.madbrains.smartyard.ui.main.address.cctv_video.adapters.DetailButtonsAdapter
import ru.madbrains.smartyard.utils.stateSharedViewModel
import timber.log.Timber

class CCTVOnlineTab : Fragment(), ExitFullscreenListener {
    private var mPlayer: SimpleExoPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val mCCTVViewModel: CCTVViewModel by stateSharedViewModel()
    private var mExoPlayerFullscreen = false

    //для полноэкранного режима
    private var lpVideoWrap: ViewGroup.LayoutParams? = null
    private var playerResizeMode: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        return inflater.inflate(R.layout.fragment_cctv_detail_online, container, false)
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (activity as? MainActivity)?.setExitFullscreenListener(null)

        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("debug_dmm __onActivityCreated")
        setupAdapter(mCCTVViewModel.cameraList.value, mCCTVViewModel.chosenIndex.value)
    }

    private fun setFullscreenMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            lpVideoWrap = LinearLayout.LayoutParams(videoWrap.layoutParams as LinearLayout.LayoutParams)
            (videoWrap.parent as ViewGroup).removeView(videoWrap)

            activity?.relativeLayout?.visibility = View.INVISIBLE
            activity?.llMain?.addView(videoWrap, 0)

            (activity as? MainActivity)?.hideSystemUI()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            playerResizeMode = mVideoView.resizeMode
            mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            mFullScreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)
            videoWrap.background = null
            activity?.llMain?.background = ColorDrawable(Color.BLACK)

            val lp = videoWrap.layoutParams as LinearLayout.LayoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            lp.topMargin = 0
            videoWrap.layoutParams = lp
            videoWrap.requestLayout()
            (mVideoView.parent as ZoomLayout).resetZoom()
        }
    }

    private fun setNormalMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            (videoWrap.parent as ViewGroup).removeView(videoWrap)
            activity?.relativeLayout?.visibility = View.VISIBLE
            llVideoPlayback.addView(videoWrap, 0)

            (activity as? MainActivity)?.showSystemUI()

            mVideoView.resizeMode = playerResizeMode
            mFullScreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)

            videoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

            //возвращаем дефолтные layouts
            if (lpVideoWrap != null) {
                videoWrap.layoutParams = lpVideoWrap
                videoWrap.requestLayout()
            }
            (mVideoView.parent as ZoomLayout).resetZoom()

            activity?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))
        }
    }

    private fun setupAdapter(currentList: List<CCTVData>?, currentIndex: Int?) {
        val lm = GridLayoutManager(context, 5)
        recyclerView.layoutManager = lm
        if (currentIndex != null && currentList != null) {
            val spacingHor = resources.getDimensionPixelSize(R.dimen.cctv_buttons_hor)
            val spacingVer = resources.getDimensionPixelSize(R.dimen.cctv_buttons_ver)
            recyclerView.addItemDecoration(GridSpacingItemDecoration(5, spacingHor, spacingVer))
            recyclerView.adapter = DetailButtonsAdapter(
                requireContext(),
                currentIndex,
                currentList
            ) {
                mCCTVViewModel.chooseCamera(it)
            }
        }
    }

    private fun setupObserve(context: Context) {
        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner,
            Observer {
                it?.run {
                    changeVideoSource(context, hls)
                }
            }
        )

        mCCTVViewModel.stateFullScreen.observe(
            viewLifecycleOwner,
            Observer {
                mExoPlayerFullscreen = it
                if (mCCTVViewModel.currentTabId == CCTVDetailFragment.ONLINE_TAB_POSITION) {
                    if (it) {
                        setFullscreenMode()
                    } else {
                        setNormalMode()
                    }
                }
            }
        )
    }

    private fun createPlayer(
        videoView: PlayerView,
        progressView: ProgressBar
    ): SimpleExoPlayer {
        Timber.d("debug_dmm create")

        val trackSelector = DefaultTrackSelector(requireContext())
        val player  = SimpleExoPlayer.Builder(requireContext())
            .setTrackSelector(trackSelector)
            .build()
        //player.addAnalyticsListener(EventLogger(trackSelector))

        videoView.player = player
        videoView.useController = false
        player.playWhenReady = true

        val p = videoView.parent as ViewGroup
        p.removeView(videoView)
        p.addView(videoView, 0)

        mFullScreen.setOnClickListener {
            mCCTVViewModel.fullScreen(!mExoPlayerFullscreen)
        }

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_READY) {
                    mPlayer?.videoFormat?.let {
                        if (it.width > 0 && it.height > 0) {
                            (mVideoView.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
                        }
                    }
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                progressView.visibility = when (playbackState) {
                    Player.STATE_BUFFERING -> View.VISIBLE
                    else -> View.GONE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                    mCCTVViewModel.showGlobalError(error.sourceException)
                }

                if (error.type == ExoPlaybackException.TYPE_RENDERER) {
                    if (forceVideoTrack) {
                        forceVideoTrack = false
                        releasePlayer()
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        initPlayer()
                    }
                }
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray) {
                super.onTracksChanged(trackGroups, trackSelections)

                if (!forceVideoTrack) {
                    return
                }

                val decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false, false)
                val maxSupportedWidth = (decoderInfo?.capabilities?.videoCapabilities?.supportedWidths?.upper ?: 0) * RESOLUTION_TOLERANCE
                val maxSupportedHeight = (decoderInfo?.capabilities?.videoCapabilities?.supportedHeights?.upper ?: 0) * RESOLUTION_TOLERANCE

                (player.trackSelector as? DefaultTrackSelector)?.let{ trackSelector ->
                    trackSelector.currentMappedTrackInfo?.let { mappedTrackInfo ->
                        for (k in 0 until mappedTrackInfo.rendererCount) {
                            if (mappedTrackInfo.getRendererType(k) == C.TRACK_TYPE_VIDEO) {
                                val rendererTrackGroups = mappedTrackInfo.getTrackGroups(k)
                                for (i in 0 until rendererTrackGroups.length) {
                                    val tracks = mutableListOf<Int>()
                                    for (j in 0 until rendererTrackGroups[i].length) {
                                        if (mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_HANDLED ||
                                            mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_EXCEEDS_CAPABILITIES &&
                                                (maxSupportedWidth >= rendererTrackGroups[i].getFormat(j).width ||
                                                maxSupportedHeight >= rendererTrackGroups[i].getFormat(j).height)) {
                                            tracks.add(j)
                                        }
                                    }
                                    val selectionOverride = DefaultTrackSelector.SelectionOverride(i, *tracks.toIntArray())
                                    trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                            .setSelectionOverride(k, rendererTrackGroups, selectionOverride)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        })
        return player
    }

    private fun changeVideoSource(context: Context, hls_url: String) {
        mPlayer?.let { player ->
            mProgress.visibility = View.VISIBLE
            player.setMediaItem(MediaItem.fromUri(Uri.parse(hls_url)))
            player.prepare()
        }
    }

    fun releasePlayer() {
        Timber.d("debug_dmm release")
        Timber.d("debug_dmm mPlayer = $mPlayer")
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
    }

    fun initPlayer() {
        if (mPlayer == null && view != null) {
            mPlayer = createPlayer(mVideoView, mProgress)
            setupObserve(requireContext())
            videoWrap.clipToOutline = true
        }
    }

    companion object {
        fun newInstance() = CCTVOnlineTab().apply {
            Timber.d("debug_dmm __new instance $this")
        }

        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
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
        if (mExoPlayerFullscreen) {
            setNormalMode()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("debug_dmm __onPause")
        releasePlayer()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        Timber.d("debug_dmm __onResume, is fragment hidden = $isHidden")

        if ((activity as? MainActivity)?.bottom_nav?.selectedItemId == R.id.address && mCCTVViewModel.currentTabId == CCTVDetailFragment.ONLINE_TAB_POSITION) {
            initPlayer()
            Timber.d("debug_dmm __CCTVOnlineTab: $this")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (mVideoView.parent as ZoomLayout).resetZoom()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        Timber.d("debug_dmm  __isVisibleToUser = $isVisibleToUser")
    }
}
