package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.lib.dpToPx
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCityCameraBinding
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.ZoomLayout
import com.sesameware.smartyard_oem.ui.main.burger.cityCameras.adapters.CityCameraEventAdapter
import com.sesameware.smartyard_oem.ui.showStandardAlert
import timber.log.Timber

class CityCameraFragment : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCityCameraBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: SimpleExoPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val viewModel: CityCamerasViewModel by sharedStateViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: LinearLayout.LayoutParams? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentCityCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivCityCameraBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.btnCityCameraEvents.setOnClickListener {
            (binding.btnCityCameraEvents.parent as ViewGroup).removeView(binding.btnCityCameraEvents)
            binding.clEvents.visibility = View.VISIBLE

            //меняем layout у некоторых элементов
            (binding.tvCityCameraTitleSub.parent as ViewGroup).removeView(binding.tvCityCameraTitleSub)
            binding.tvCityCameraTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f)
            val lp = binding.tvCityCameraTitle.layoutParams as ConstraintLayout.LayoutParams
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET
            lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
            lp.startToEnd = R.id.ivCityCameraBack
            lp.topToTop = R.id.ivCityCameraBack
            lp.topMargin = 16.dpToPx()
            binding.tvCityCameraTitle.layoutParams = lp
            binding.tvCityCameraTitle.requestLayout()

            val lp2 = binding.llCityCameraMain.layoutParams as ConstraintLayout.LayoutParams
            lp2.topToBottom = R.id.ivCityCameraBack
            binding.llCityCameraMain.layoutParams = lp2
            binding.llCityCameraMain.requestLayout()
        }
        if (viewModel.isFullscreen) {
            setFullScreenMode()
        }

        binding.zlCityCamera.setSingleTapConfirmeListener {
            if (mPlayer?.playbackState == Player.STATE_IDLE) {
                changeVideoSource(requireContext(), viewModel.chosenCamera.value?.hls ?: "")
            }
        }

        binding.btnCityCameraRequestRecord.setOnClickListener {
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
        if ((activity as? MainActivity)?.binding?.bottomNav?.selectedItemId == R.id.settings) {
            initPlayer()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (binding.pvCityCamera.parent as ZoomLayout).resetZoom()
    }

    override fun onExitFullscreen() {
        if (viewModel.isFullscreen) {
            setNormalMode()
        }
    }

    private fun setupObservers() {
        viewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) {
            it?.run {
                val slash = this.name.indexOf("/")
                if (0 < slash && slash < this.name.length - 1) {
                    binding.tvCityCameraTitle.text = this.name.substring(0, slash).trim()
                    binding.tvCityCameraTitleSub.text = this.name.substring(slash + 1).trim()
                } else {
                    binding.tvCityCameraTitle.text = this.name;
                    binding.tvCityCameraTitleSub.text = this.name;
                }

                viewModel.getEvents(it.id) {
                    if (viewModel.eventList.isEmpty()) {
                        binding.btnCityCameraEvents.text =
                            resources.getString(R.string.city_camera_events)
                    } else {
                        binding.btnCityCameraEvents.text = resources.getString(
                            R.string.city_camera_events_count,
                            viewModel.eventList.size
                        )
                    }
                    setupEventAdapter()
                }
            }
        }
    }

    private fun setupEventAdapter() {
        val llm = LinearLayoutManager(requireContext())
        binding.rvCityCameraEvents.layoutManager = llm
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
        binding.rvCityCameraEvents.adapter = adapter
        binding.rvCityCameraEvents.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val i1 = llm.findFirstCompletelyVisibleItemPosition()
                val i2 = llm.findLastCompletelyVisibleItemPosition()

                if (i2 == viewModel.eventList.size - 1 && i1 > 0) {
                    binding.ivCityCameraArrowUp.visibility = View.VISIBLE
                    binding.tvCityCameraScroll.text = resources.getString(R.string.city_camera_scroll_up)
                    binding.llCityCameraBottom.visibility = View.VISIBLE
                } else {
                    if (i2 == adapter.currentSize() - 1 && adapter.currentSize() < viewModel.eventList.size) {
                        binding.ivCityCameraArrowUp.visibility = View.INVISIBLE
                        val diff = viewModel.eventList.size - adapter.currentSize()
                        val more =
                            if (diff < CityCamerasViewModel.CHUNK_ITEM_COUNT) diff else CityCamerasViewModel.CHUNK_ITEM_COUNT
                        binding.tvCityCameraScroll.text =
                            resources.getString(R.string.city_camera_load_more, more)
                        binding.llCityCameraBottom.visibility = View.VISIBLE
                    } else {
                        binding.llCityCameraBottom.visibility = View.INVISIBLE
                    }
                }
            }
        })

        binding.llCityCameraBottom.setOnClickListener {
            if (adapter.currentSize() < viewModel.eventList.size) {
                adapter.setCurrentSize(adapter.currentSize() + CityCamerasViewModel.CHUNK_ITEM_COUNT)
            } else {
                llm.scrollToPosition(0)
            }
            binding.llCityCameraBottom.visibility = View.INVISIBLE
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
            mPlayer = createPlayer(binding.pvCityCamera, binding.pbCityCamera)
        }
        binding.flCityCameraVideoWrap.clipToOutline = true
        
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

        binding.ivCityCameraFullscreen.setOnClickListener {
            viewModel.isFullscreen = !viewModel.isFullscreen
            if (viewModel.isFullscreen) {
                setFullScreenMode()
            } else {
                setNormalMode()
            }
        }

        player.addListener(object : Player.EventListener {
            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_READY) {
                    mPlayer?.videoFormat?.let {
                        if (it.width > 0 && it.height > 0) {
                            (binding.pvCityCamera.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
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
            binding.pbCityCamera.visibility = View.VISIBLE
            player.setMediaItem(MediaItem.fromUri(Uri.parse(hls_url)))
            player.prepare()
        }
    }

    private fun setFullScreenMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        //сохраняем дефолтный layout
        lpVideoWrap = LinearLayout.LayoutParams(binding.flCityCameraVideoWrap.layoutParams as LinearLayout.LayoutParams)

        binding.pbCityCamera.progress = 0
        binding.ivCityCameraFullscreen.visibility = View.VISIBLE
        binding.pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.ivCityCameraFullscreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)
        binding.flCityCameraVideoWrap.background = null
        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

        (activity as? MainActivity)?.hideSystemUI()
        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.INVISIBLE
        (binding.flCityCameraVideoWrap.parent as ViewGroup).removeView(binding.flCityCameraVideoWrap)
        (activity as? MainActivity)?.binding?.llMain?.addView(binding.flCityCameraVideoWrap, 0)

        val lp = binding.flCityCameraVideoWrap.layoutParams as LinearLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = 0
        binding.flCityCameraVideoWrap.layoutParams = lp
        binding.flCityCameraVideoWrap.requestLayout()
        (binding.pvCityCamera.parent as ZoomLayout).resetZoom()
    }

    private fun setNormalMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (binding.flCityCameraVideoWrap.parent as ViewGroup).removeView(binding.flCityCameraVideoWrap)
        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.VISIBLE
        binding.llCityCameraMain.addView(binding.flCityCameraVideoWrap, 0)
        (activity as? MainActivity)?.showSystemUI()
        binding.pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        binding.ivCityCameraFullscreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)
        binding.flCityCameraVideoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

        //возвращаем дефолтные layouts
        lpVideoWrap?.let { lp ->
            lp.height = 222.dpToPx()
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.marginStart = 16.dpToPx()
            lp.marginEnd = 16.dpToPx()
            lp.topMargin = 16.dpToPx()
            binding.flCityCameraVideoWrap.layoutParams = lp
            binding.flCityCameraVideoWrap.requestLayout()
        }
        (binding.pvCityCamera.parent as ZoomLayout).resetZoom()
        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __onHiddenChanged hidden = $hidden")

        if (hidden) {
            releasePlayer()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (mPlayer == null && view != null) {
                mPlayer = createPlayer(binding.pvCityCamera, binding.pbCityCamera)
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
