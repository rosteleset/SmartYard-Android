package ru.madbrains.smartyard.ui.main.burger.cityCameras

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_city_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.madbrains.lib.dpToPx
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.ExitFullscreenListener
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.cctv_video.ZoomLayout
import ru.madbrains.smartyard.ui.main.burger.cityCameras.adapters.CityCameraEventAdapter
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.madbrains.smartyard.utils.stateSharedViewModel
import timber.log.Timber

class CityCameraFragment : Fragment(), ExitFullscreenListener {
    private var mPlayer: SimpleExoPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val viewModel: CityCamerasViewModel by stateSharedViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: LinearLayout.LayoutParams? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        return inflater.inflate(R.layout.fragment_city_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCityCameraBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        btnCityCameraEvents.setOnClickListener {
            (btnCityCameraEvents.parent as ViewGroup).removeView(btnCityCameraEvents)
            clEvents.visibility = View.VISIBLE

            //меняем layout у некоторых элементов
            (tvCityCameraTitleSub.parent as ViewGroup).removeView(tvCityCameraTitleSub)
            tvCityCameraTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f)
            val lp = tvCityCameraTitle.layoutParams as ConstraintLayout.LayoutParams
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET
            lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
            lp.startToEnd = R.id.ivCityCameraBack
            lp.topToTop = R.id.ivCityCameraBack
            lp.topMargin = 16.dpToPx()
            tvCityCameraTitle.layoutParams = lp
            tvCityCameraTitle.requestLayout()

            val lp2 = llCityCameraMain.layoutParams as ConstraintLayout.LayoutParams
            lp2.topToBottom = R.id.ivCityCameraBack
            llCityCameraMain.layoutParams = lp2
            llCityCameraMain.requestLayout()
        }
        if (viewModel.isFullscreen) {
            setFullScreenMode()
        }

        zlCityCamera?.setSingleTapConfirmeListener {
            if (mPlayer?.playbackState == Player.STATE_IDLE) {
                changeVideoSource(requireContext(), viewModel.chosenCamera.value?.hls ?: "")
            }
        }

        btnCityCameraRequestRecord?.setOnClickListener {
            this.findNavController().navigate(R.id.action_cityCameraFragment_to_requestRecordFragment)
        }

        setupObservers()
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (activity as? MainActivity)?.setExitFullscreenListener(null)

        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()

        Timber.d("debug_dmm __onPause")
        releasePlayer()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        Timber.d("debug_dmm __onResume")
        if ((activity as? MainActivity)?.bottom_nav?.selectedItemId == R.id.settings) {
            initPlayer()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (pvCityCamera.parent as ZoomLayout).resetZoom()
    }

    override fun onExitFullscreen() {
        if (viewModel.isFullscreen) {
            setNormalMode()
        }
    }

    private fun setupObservers() {
        viewModel.chosenCamera.observe(
            viewLifecycleOwner,
            {
                it?.run {
                    val slash = this.name.indexOf("/")
                    if (0 < slash && slash < this.name.length - 1) {
                        tvCityCameraTitle.text = this.name.substring(0, slash).trim()
                        tvCityCameraTitleSub.text = this.name.substring(slash + 1).trim()
                    } else {
                        tvCityCameraTitle.text = this.name;
                        tvCityCameraTitleSub.text = this.name;
                    }

                    viewModel.getEvents(it.id) {
                        if (viewModel.eventList.isEmpty()) {
                            btnCityCameraEvents.text = resources.getString(R.string.city_camera_events)
                        } else {
                            btnCityCameraEvents.text = resources.getString(R.string.city_camera_events_count, viewModel.eventList.size)
                        }
                        setupEventAdapter()
                    }
                }
            }
        )
    }

    private fun setupEventAdapter() {
        val llm = LinearLayoutManager(requireContext())
        rvCityCameraEvents.layoutManager = llm
        val adapter = CityCameraEventAdapter(viewModel.eventList, CityCamerasViewModel.CHUNK_ITEM_COUNT) {
            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.eventList[it].url))
            try {
                startActivity(youtubeIntent)
            } catch (e: ActivityNotFoundException) {
                showStandardAlert(requireContext(),
                    resources.getString(R.string.error),
                    resources.getString(R.string.city_camera_youtube_msg))
            }
        }
        rvCityCameraEvents.adapter = adapter
        rvCityCameraEvents.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val i1 = llm.findFirstCompletelyVisibleItemPosition()
                val i2 = llm.findLastCompletelyVisibleItemPosition()

                if (i2 == viewModel.eventList.size - 1 && i1 > 0) {
                    ivCityCameraArrowUp.visibility = View.VISIBLE
                    tvCityCameraScroll.text = resources.getString(R.string.city_camera_scroll_up)
                    llCityCameraBottom.visibility = View.VISIBLE
                } else {
                    if (i2 == adapter.currentSize() - 1 && adapter.currentSize() < viewModel.eventList.size) {
                        ivCityCameraArrowUp.visibility = View.INVISIBLE
                        val diff = viewModel.eventList.size - adapter.currentSize()
                        val more =
                            if (diff < CityCamerasViewModel.CHUNK_ITEM_COUNT) diff else CityCamerasViewModel.CHUNK_ITEM_COUNT
                        tvCityCameraScroll.text =
                            resources.getString(R.string.city_camera_load_more, more)
                        llCityCameraBottom.visibility = View.VISIBLE
                    } else {
                        llCityCameraBottom.visibility = View.INVISIBLE
                    }
                }
            }
        })

        llCityCameraBottom.setOnClickListener {
            if (adapter.currentSize() < viewModel.eventList.size) {
                adapter.setCurrentSize(adapter.currentSize() + CityCamerasViewModel.CHUNK_ITEM_COUNT)
            } else {
                llm.scrollToPosition(0)
            }
            llCityCameraBottom.visibility = View.INVISIBLE
        }
    }

    private fun releasePlayer() {
        Timber.d("debug_dmm release")
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
    }

    private fun initPlayer() {
        if (mPlayer == null && view != null) {
            mPlayer = createPlayer(pvCityCamera, pbCityCamera)
        }
        flCityCameraVideoWrap.clipToOutline = true
        
        loadDelayed(LOADING_VIDEO_DELAY)
    }

    private fun loadDelayed(delayMs: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(delayMs)
            withContext(Dispatchers.Main) {
                changeVideoSource(requireContext(), viewModel.chosenCamera.value?.hls ?: "")
            }
        }
    }

    private fun createPlayer(
        videoView: PlayerView,
        progressView: ProgressBar
    ): SimpleExoPlayer {
        Timber.d("debug_dmm create")
        
        val trackSelector = DefaultTrackSelector(requireContext())
        /*val params = trackSelector.buildUponParameters()
            .setForceHighestSupportedBitrate(true)
            .setMaxVideoSize(4000, 3000)
            .build()
        trackSelector.parameters = params*/
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

        ivCityCameraFullscreen.setOnClickListener {
            viewModel.isFullscreen = !viewModel.isFullscreen
            if (viewModel.isFullscreen) {
                setFullScreenMode()
            } else {
                setNormalMode()
            }
        }

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_READY) {
                    mPlayer?.videoFormat?.let {
                        if (it.width > 0 && it.height > 0) {
                            (pvCityCamera.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
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
                    viewModel.showGlobalError(error.sourceException)
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
        if (hls_url.isEmpty()) {
            return
        }

        mPlayer?.let { player ->
            pbCityCamera.visibility = View.VISIBLE
            player.setMediaItem(MediaItem.fromUri(Uri.parse(hls_url)))
            player.prepare()
        }
    }

    private fun setFullScreenMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        //сохраняем дефолтный layout
        lpVideoWrap = LinearLayout.LayoutParams(flCityCameraVideoWrap.layoutParams as LinearLayout.LayoutParams)

        pbCityCamera.progress = 0
        ivCityCameraFullscreen.visibility = View.VISIBLE
        pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        ivCityCameraFullscreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)
        flCityCameraVideoWrap.background = null
        activity?.llMain?.background = ColorDrawable(Color.BLACK)

        (activity as? MainActivity)?.hideSystemUI()
        activity?.relativeLayout?.visibility = View.INVISIBLE
        (flCityCameraVideoWrap.parent as ViewGroup).removeView(flCityCameraVideoWrap)
        activity?.llMain?.addView(flCityCameraVideoWrap, 0)

        val lp = flCityCameraVideoWrap.layoutParams as LinearLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = 0
        flCityCameraVideoWrap.layoutParams = lp
        flCityCameraVideoWrap.requestLayout()
        (pvCityCamera.parent as ZoomLayout).resetZoom()
    }

    private fun setNormalMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (flCityCameraVideoWrap.parent as ViewGroup).removeView(flCityCameraVideoWrap)
        activity?.relativeLayout?.visibility = View.VISIBLE
        llCityCameraMain.addView(flCityCameraVideoWrap, 0)
        (activity as? MainActivity)?.showSystemUI()
        pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        ivCityCameraFullscreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)
        flCityCameraVideoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

        //возвращаем дефолтные layouts
        lpVideoWrap?.let { lp ->
            lp.height = 222.dpToPx()
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.marginStart = 16.dpToPx()
            lp.marginEnd = 16.dpToPx()
            lp.topMargin = 16.dpToPx()
            flCityCameraVideoWrap.layoutParams = lp
            flCityCameraVideoWrap.requestLayout()
        }
        (pvCityCamera.parent as ZoomLayout).resetZoom()
        activity?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __onHiddenChanged hidden = $hidden")

        if (hidden) {
            releasePlayer()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (mPlayer == null && view != null) {
                mPlayer = createPlayer(pvCityCamera, pbCityCamera)
                loadDelayed(0L)
            }
        }

        super.onHiddenChanged(hidden)
    }

    companion object {
        const val LOADING_VIDEO_DELAY = 1000L
        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
    }
}