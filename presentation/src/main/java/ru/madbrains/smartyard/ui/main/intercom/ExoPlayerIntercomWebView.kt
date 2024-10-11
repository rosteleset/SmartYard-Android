package ru.madbrains.smartyard.ui.main.intercom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import ru.madbrains.lib.dpToPx
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentExoPlayerIntercomWebViewBinding
import ru.madbrains.smartyard.ui.main.ExitFullscreenListener
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.cctv_video.ZoomLayout
import ru.madbrains.smartyard.ui.main.burger.cityCameras.CityCamerasViewModel
import timber.log.Timber

class ExoPlayerIntercomWebView : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentExoPlayerIntercomWebViewBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: ExoPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val viewModel: CityCamerasViewModel by sharedStateViewModel()
    private var url = ""
    private var domophoneId = 0L
    private var doorId = 0
    private var title = ""
    private var name = ""

    //для полноэкранного режима
    private var lpVideoWrap: LinearLayout.LayoutParams? = null
    private var mPlayerView: PlayerView? = null

    private var startY = 0f
    private val SWIPE_THRESHOLD = 450 // Минимальное расстояние для определения свайпа
    private val TOUCH_EVENT_THRESHOLD =
        5  // Минимальное расстояние между двумя событиями ACTION_DOWN и ACTION_MOVE для их связывания


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentExoPlayerIntercomWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        url = arguments?.getString("uriHls") ?: ""
        domophoneId = arguments?.getLong("domophoneId") ?: 0L
        doorId = arguments?.getInt("doorId") ?: 0
        title = arguments?.getString("title") ?: ""
        name = arguments?.getString("name") ?: ""
        initFragment(url)

        if (domophoneId != 0L) {
            binding.ibOpenDoor.isVisible = true
            binding.tvTitleAddress.text = title
            binding.ibOpenDoor.setOnClickListener {
                binding.ibOpenDoor.isClickable = false
                binding.ibOpenDoor.setImageResource(R.drawable.ic_open)
                val countDownTimer = object : CountDownTimer(8000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        binding.ibOpenDoor.isClickable = true
                        binding.ibOpenDoor.setImageResource(R.drawable.ic_open_no_active)
                    }
                }
                countDownTimer.start()
                viewModel.openDoor(domophoneId, doorId)
            }
        }

//        //Swipe Up/Down to close player
//        binding.zlCityCamera.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    startY = event.y
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val deltaY = event.y - startY
//                    if (Math.abs(deltaY) > TOUCH_EVENT_THRESHOLD) {
//                        if (deltaY > SWIPE_THRESHOLD) {
//                            setNormalMode()
//                            parentFragmentManager.popBackStack()
//                        } else if (deltaY < -SWIPE_THRESHOLD) {
//                            setNormalMode()
//                            parentFragmentManager.popBackStack()
//                        }
//                    }
//                }
//            }
//            true // Важно вернуть true, чтобы указать, что событие обработано
//        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            setNormalMode()
            parentFragmentManager.popBackStack()
        }
        setFullScreenMode()
        binding.zlCityCamera.setSingleTapConfirmeListener {
            if (mPlayer?.playbackState == Player.STATE_IDLE) {
                changeVideoSource(requireContext(), viewModel.chosenCamera.value?.hls ?: "")
            }
        }
    }


    private fun setNormalModeIntent() {
        setNormalMode()
        parentFragmentManager.popBackStack()
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


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (binding.pvCityCamera.parent as ZoomLayout).resetZoom()
    }

    override fun onExitFullscreen() {
        if (viewModel.isFullscreen) {
            setNormalMode()
        }
    }


    fun initFragment(url: String) {
        this.url = url
        initPlayer(url)
    }


    private fun releasePlayer() {
        Timber.d("debug_dmm release")
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
    }

    private fun initPlayer(url: String) {
        if (mPlayer == null && view != null) {
            mPlayer = createPlayer(binding.pvCityCamera, binding.pbCityCamera)
        }

        mPlayer?.volume = 0f

        binding.flCityCameraVideoWrap.clipToOutline = true

        loadDelayed(LOADING_VIDEO_DELAY, url)
    }

    private fun loadDelayed(delayMs: Long, url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(delayMs)
            withContext(Dispatchers.Main) {
                changeVideoSource(requireContext(), url ?: "")
            }
        }
    }

    private fun createPlayer(
        videoView: PlayerView,
        progressView: ProgressBar
    ): ExoPlayer {
        Timber.d("debug_dmm create")

        val trackSelector = DefaultTrackSelector(requireContext())
        /*val params = trackSelector.buildUponParameters()
            .setForceHighestSupportedBitrate(true)
            .setMaxVideoSize(4000, 3000)
            .build()
        trackSelector.parameters = params*/
        val player = ExoPlayer.Builder(requireContext())
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

        player.addListener(object : Player.Listener {
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

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                if (error.errorCode == ExoPlaybackException.TYPE_SOURCE) {
                    viewModel.showGlobalError(error)
                }

                if (error.errorCode == ExoPlaybackException.TYPE_RENDERER) {
                    if (forceVideoTrack) {
                        forceVideoTrack = false
                        releasePlayer()
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        initPlayer(url)
                    }
                }

            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)
                if (!forceVideoTrack) {
                    return
                }

                val decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false, false)
                val maxSupportedWidth =
                    (decoderInfo?.capabilities?.videoCapabilities?.supportedWidths?.upper
                        ?: 0) * RESOLUTION_TOLERANCE
                val maxSupportedHeight =
                    (decoderInfo?.capabilities?.videoCapabilities?.supportedHeights?.upper
                        ?: 0) * RESOLUTION_TOLERANCE

                (player.trackSelector as? DefaultTrackSelector)?.let { trackSelector ->
                    trackSelector.currentMappedTrackInfo?.let { mappedTrackInfo ->
                        for (k in 0 until mappedTrackInfo.rendererCount) {
                            if (mappedTrackInfo.getRendererType(k) == C.TRACK_TYPE_VIDEO) {
                                val rendererTrackGroups = mappedTrackInfo.getTrackGroups(k)
                                for (i in 0 until rendererTrackGroups.length) {
                                    val tracks = mutableListOf<Int>()
                                    for (j in 0 until rendererTrackGroups[i].length) {
                                        if (mappedTrackInfo.getTrackSupport(
                                                k,
                                                i,
                                                j
                                            ) == C.FORMAT_HANDLED ||
                                            mappedTrackInfo.getTrackSupport(
                                                k,
                                                i,
                                                j
                                            ) == C.FORMAT_EXCEEDS_CAPABILITIES &&
                                            (maxSupportedWidth >= rendererTrackGroups[i].getFormat(j).width ||
                                                    maxSupportedHeight >= rendererTrackGroups[i].getFormat(
                                                j
                                            ).height)
                                        ) {
                                            tracks.add(j)
                                        }
                                    }
                                    val selectionOverride = DefaultTrackSelector.SelectionOverride(
                                        i,
                                        *tracks.toIntArray()
                                    )
                                    trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                            .setSelectionOverride(
                                                k,
                                                rendererTrackGroups,
                                                selectionOverride
                                            )
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
        lpVideoWrap =
            LinearLayout.LayoutParams(binding.flCityCameraVideoWrap.layoutParams as LinearLayout.LayoutParams)

        binding.pbCityCamera.progress = 0
        binding.ivCityCameraFullscreen.visibility = View.VISIBLE
        binding.pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.ivCityCameraFullscreen.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)
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
        ////////////////////////////////////////////////
        binding.ivVolumeBtn.setOnClickListener {
            if (mPlayer?.volume == 0f) {
                binding.ivVolumeBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.baseline_volume_up_24)
                mPlayer?.volume = 1f
            } else {
                binding.ivVolumeBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.baseline_volume_off_24)
                mPlayer?.volume = 0f
            }

        }
        binding.ivCityCameraFullscreen.setOnClickListener {
            setNormalMode()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setNormalMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (binding.flCityCameraVideoWrap.parent as ViewGroup).removeView(binding.flCityCameraVideoWrap)
        (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.VISIBLE
        binding.llCityCameraMain.addView(binding.flCityCameraVideoWrap, 0)
        (activity as? MainActivity)?.showSystemUI()
        binding.pvCityCamera.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        binding.ivCityCameraFullscreen.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)
        binding.flCityCameraVideoWrap.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

        //возвращаем дефолтные layouts
        lpVideoWrap?.let { lp ->
            lp.height = 222.dpToPx()
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
//            lp.marginStart = 16.dpToPx()
//            lp.marginEnd = 16.dpToPx()
            lp.topMargin = 16.dpToPx()
            binding.flCityCameraVideoWrap.layoutParams = lp
            binding.flCityCameraVideoWrap.requestLayout()
        }
        (binding.pvCityCamera.parent as ZoomLayout).resetZoom()
        (activity as? MainActivity)?.binding?.llMain?.background =
            ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))


    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __onHiddenChanged hidden = $hidden")

        if (hidden) {
            releasePlayer()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (mPlayer == null && view != null) {
                mPlayer = createPlayer(binding.pvCityCamera, binding.pbCityCamera)
                loadDelayed(0L, url)
            }
        }

        super.onHiddenChanged(hidden)
    }





    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == SET_NORMAL_MODE_PLAYER) {
                    setNormalModeIntent()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SET_NORMAL_MODE_PLAYER)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                intentFilter
            )
        }
    }



    companion object {
        const val SET_NORMAL_MODE_PLAYER = "SET_NORMAL_MODE_PLAYER"
        const val LOADING_VIDEO_DELAY = 1000L
        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
    }
}
