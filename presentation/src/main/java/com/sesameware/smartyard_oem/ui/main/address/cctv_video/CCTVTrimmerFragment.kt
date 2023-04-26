package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.MediaServerType
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.lib.TimeInterval
import com.sesameware.lib.timeInMs
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.clamp
import com.sesameware.smartyard_oem.databinding.FragmentCctvTrimmerBinding
import com.sesameware.smartyard_oem.removeTrailingZeros
import com.sesameware.smartyard_oem.show
import com.sesameware.smartyard_oem.ui.animationFadeInFadeOut
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.UserInteractionListener
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVTrimmerViewModel.Companion.dialogPrepareVideo
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.adapters.TimeFragmentButtonsAdapter
import com.sesameware.smartyard_oem.ui.showStandardAlert
import timber.log.Timber

class CCTVTrimmerFragment : Fragment(), UserInteractionListener {
    private var _binding: FragmentCctvTrimmerBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: BaseCCTVPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private lateinit var chosenDate: LocalDate

    private val mDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private val mViewModel by viewModel<CCTVTrimmerViewModel>()
    private var mCurrentPlaybackData: CCTVTrimmerViewModel.PlayerIntervalChangeData? = null

    private var mExoPlayerView: PlayerView? = null
    private var mExoPlayerFullscreen = false

    //для полноэкранного режима
    private var lpContentWrap: ViewGroup.LayoutParams? = null
    private var lpVideoWrap: ViewGroup.LayoutParams? = null
    private var lpRangeSlider: ViewGroup.LayoutParams? = null
    private var playerResizeMode: Int = 0
    private var areVideoControllersShown = false
    private val hideVideoControllersHandler = Handler()
    private val hideVideoControllersRunnable = Runnable {
        hideVideoControllers()
    }

    private var canRenewToken = true

    //список временных интервалов архива, которые есть на сервере, внутри выбранного пользователем интервала просмотра архива
    private var archiveRanges = mutableListOf<TimeInterval>()

    //индекс проигрываемого в данный момент архивного интервала
    private var currentArchiveRangeIndex = -1

    private fun generateArchiveRanges() {
        archiveRanges.clear()

        mCurrentPlaybackData?.interval?.run {
            val rangeFrom = this.from
            val rangeTo = this.to

            //ищем пересечения с интервалами из архива
            mCCTVViewModel.availableRanges.forEach {range ->
                if ((rangeFrom < range.endDate) && (range.startDate < rangeTo)) {
                    val intersectionLeft = if (rangeFrom > range.startDate) rangeFrom else range.startDate
                    val intersectionRight = if (rangeTo < range.endDate) rangeTo else range.endDate
                    archiveRanges.add(TimeInterval(intersectionLeft, intersectionRight, DataModule.serverTz))
                }
            }
        }

        mViewModel.setAvailableRanges(mCCTVViewModel.availableRanges)
        binding.rangePlayer.setAvailableIntervals(mCurrentPlaybackData?.interval, archiveRanges)
        //rangePlayer.setBarHeight(2.dpToPx())
    }

    private fun generateTrimmerRanges(interval: TimeInterval) {
        val trimmerRanges = mutableListOf<TimeInterval>()
        val rangeFrom = interval.from
        val rangeTo = interval.to

        //ищем пересечения с интервалами из архива
        mCCTVViewModel.availableRanges.forEach {range ->
            if ((rangeFrom < range.endDate) && (range.startDate < rangeTo)) {
                val intersectionLeft = if (rangeFrom > range.startDate) rangeFrom else range.startDate
                val intersectionRight = if (rangeTo < range.endDate) rangeTo else range.endDate
                trimmerRanges.add(TimeInterval(intersectionLeft, intersectionRight, DataModule.serverTz))
            }
        }

        binding.rangeTrimmer.setAvailableIntervals(interval, trimmerRanges)
    }

    //позиционирование в указанную временную точку в миллисекундах внутри интервала
    private fun playerSeekTo(ms: Long) {
        Timber.d("__Q__  call  playerSeekTo = $ms")

        if (archiveRanges.size == 0) {
            return
        }

        if (mPlayer == null) {
            return
        }

        var seekToTime = archiveRanges.first().from.plusSeconds(ms / 1000)

        //ищем индекс архивного интервала
        var foundIndex = -1
        run loop@ {
            archiveRanges.forEachIndexed { index, timeInterval ->
                if (timeInterval.from <= seekToTime && seekToTime < timeInterval.to) {
                    foundIndex = index
                    return@loop
                }

                if (seekToTime < timeInterval.from) {
                    foundIndex = index
                    return@loop
                }
            }
        }

        if (foundIndex < 0) {
            foundIndex = 0
            seekToTime = archiveRanges.first().from
        }

        var isNewMedia = false
        if (foundIndex != currentArchiveRangeIndex) {
            currentArchiveRangeIndex = foundIndex
            isNewMedia = true
        }

        if (seekToTime < archiveRanges[currentArchiveRangeIndex].from) {
            seekToTime = archiveRanges[currentArchiveRangeIndex].from
        }

        val seekMediaTo = seekToTime.timeInMs() - archiveRanges[currentArchiveRangeIndex].from.timeInMs()
        if (isNewMedia) {
            prepareMedia(seekMediaTo, mPlayer?.playWhenReady ?: false)
        } else {
            mPlayer?.seekTo(seekToTime.timeInMs() - archiveRanges[currentArchiveRangeIndex].from.timeInMs())
        }
        binding.rangePlayer.slider.setSeekFromPlayer(playerCurrentPosition())
    }

    //текущая позиция видео в миллисекундах внутри интервала
    private fun playerCurrentPosition(): Long {
        if (archiveRanges.size == 0) {
            return 0L
        }

        if (mPlayer == null) {
            return 0L
        }

        if (currentArchiveRangeIndex < 0) {
            return 0L
        }

        return archiveRanges[currentArchiveRangeIndex].from.timeInMs() - archiveRanges.first().from.timeInMs() + (mPlayer?.currentPosition() ?: 0)
    }

    //общая длительность интервала
    private fun playerDuration(): Long {
        return (mCurrentPlaybackData?.interval?.durationInMs ?: 0)
    }

    companion object {
        private const val MAX_SPEED_UP_VALUE = 16.0
        private const val MIN_SPEED_DOWN_VALUE = 0.25
        private const val prepareVideoWait = 1L

        const val INACTIVE_PERIOD_MS = 5_000L  //период бездествия пользователя в миллисекундах, спустя который скрываются видео контроллеры
    }

    override fun onUserInteraction() {
        if (mExoPlayerFullscreen) {
            resetInactiveTimer()
        }
    }

    private fun resetInactiveTimer() {
        hideVideoControllersHandler.removeCallbacks(hideVideoControllersRunnable)
        hideVideoControllersHandler.postDelayed(hideVideoControllersRunnable, INACTIVE_PERIOD_MS)
    }

    private fun hideVideoControllers() {
        if (mExoPlayerFullscreen) {
            binding.contentWrap.visibility = View.GONE
            binding.mFullScreens.visibility = View.GONE
            areVideoControllersShown = false
        }
    }

    private fun showVideoControllers() {
        if (mExoPlayerFullscreen) {
            binding.contentWrap.visibility = View.VISIBLE
            binding.mFullScreens.visibility = View.VISIBLE
            areVideoControllersShown = true
            resetInactiveTimer()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MainActivity)?.setUserInteractionListener(this)
        _binding = FragmentCctvTrimmerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        if (mExoPlayerFullscreen) {
            (activity as? MainActivity)?.showSystemUI()
        }
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (activity as? MainActivity)?.setUserInteractionListener(null)
        
        super.onDestroyView()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireNotNull(arguments).let {
            chosenDate = CCTVTrimmerFragmentArgs.fromBundle(it).chosenDate
        }

        val camera = mCCTVViewModel.chosenCamera.value

        //для теста
        Timber.d("debug_dmm ActivityCreated: $camera")

        val initialThumb = mCCTVViewModel.initialThumb
        camera?.let { cam ->
            mViewModel.initialize(cam, initialThumb)
        }
        context?.let { context ->
            setupUi(context)
            setupObserve(context)
        }
    }

    private fun setFullscreenMode() {
        lpContentWrap = ConstraintLayout.LayoutParams(binding.contentWrap.layoutParams as ConstraintLayout.LayoutParams)

        lpVideoWrap = LinearLayout.LayoutParams(binding.videoWrap.layoutParams as LinearLayout.LayoutParams)
        (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
        binding.clVideoPlayback.addView(binding.videoWrap, 1)

        lpRangeSlider = FrameLayout.LayoutParams(binding.rangePlayer.layoutParams as FrameLayout.LayoutParams)
        (binding.rangePlayer.parent as ViewGroup).removeView(binding.rangePlayer)
        binding.llControls.addView(binding.rangePlayer, 0)

        (binding.panelTrim.parent as ViewGroup).removeView(binding.panelTrim)
        (binding.btnMainAction.parent as ViewGroup).removeView(binding.btnMainAction)

        //скрываем ненужные элементы
        binding.imageView.visibility = View.INVISIBLE
        binding.ivBack.visibility = View.INVISIBLE
        binding.tvTitle.visibility = View.INVISIBLE
        binding.gradStart.visibility = View.INVISIBLE
        binding.gradEnd.visibility = View.INVISIBLE
        (activity as? MainActivity)?.hideSystemUI()

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        binding.contentWrap.background = null
        binding.svContentWrap.background = null
        binding.flVideoPlayback.setBackgroundColor(Color.BLACK)
        binding.llControls.background = ColorDrawable(Color.parseColor("#77000000"))

        playerResizeMode = binding.mPlayerView.resizeMode
        binding.mPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.mFullScreens.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)

        //layouts в полноэкранном режиме
        val lp2 = binding.videoWrap.layoutParams as ConstraintLayout.LayoutParams
        lp2.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        lp2.height = ConstraintLayout.LayoutParams.MATCH_PARENT
        lp2.topToTop = R.id.clVideoPlayback
        lp2.bottomToBottom = R.id.clVideoPlayback
        lp2.startToStart = R.id.clVideoPlayback
        lp2.endToEnd = R.id.clVideoPlayback
        binding.videoWrap.layoutParams = lp2
        binding.videoWrap.requestLayout()

        val lp = (binding.contentWrap.layoutParams as ConstraintLayout.LayoutParams)
        lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.startToStart = R.id.clVideoPlayback
        lp.endToEnd = R.id.clVideoPlayback
        lp.bottomToBottom = R.id.clVideoPlayback
        lp.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        binding.contentWrap.layoutParams = lp
        binding.contentWrap.requestLayout()
        (binding.mPlayerView.parent as ZoomLayout).resetZoom()

        (binding.rvTimeFragmentButtons.adapter as TimeFragmentButtonsAdapter).setFullscreen(true)
        binding.tvSpeedUp.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_0))
        binding.tvSpeedDown.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_0))
        binding.btnPlay.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_p_button_fs)

        areVideoControllersShown = true
        resetInactiveTimer()

        binding.rangePlayer.setupRV(requireContext())
    }

    private fun setNormalMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            binding.contentWrap.visibility = View.VISIBLE
            binding.mFullScreens.visibility = View.VISIBLE
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
            binding.llControls.addView(binding.videoWrap, 0)

            (binding.rangePlayer.parent as ViewGroup).removeView(binding.rangePlayer)
            binding.videoWrap.addView(binding.rangePlayer, 2)

            binding.llControls.addView(binding.panelTrim)
            binding.llControls.addView(binding.btnMainAction)

            //показываем скрытые элементы
            binding.imageView.visibility = View.VISIBLE
            binding.ivBack.visibility = View.VISIBLE
            binding.tvTitle.visibility = View.VISIBLE
            binding.gradStart.visibility = View.VISIBLE
            binding.gradEnd.visibility = View.VISIBLE
            (activity as? MainActivity)?.showSystemUI()

            binding.contentWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_upper_clip)
            binding.svContentWrap.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white))
            binding.flVideoPlayback.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.llControls.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white))

            binding.mPlayerView.resizeMode = playerResizeMode
            binding.mFullScreens.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)

            //возвращаем дефолтные layouts
            binding.videoWrap.layoutParams = lpVideoWrap
            binding.videoWrap.requestLayout()

            binding.rangePlayer.layoutParams = lpRangeSlider
            binding.rangePlayer.requestLayout()

            binding.contentWrap.layoutParams = lpContentWrap
            binding.contentWrap.requestLayout()
            (binding.mPlayerView.parent as ZoomLayout).resetZoom()

            (binding.rvTimeFragmentButtons.adapter as TimeFragmentButtonsAdapter).setFullscreen(false)
            binding.tvSpeedUp.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_100))
            binding.tvSpeedDown.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_100))
            binding.btnPlay.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_p_button)

            binding.rangePlayer.setupRV(requireContext())
        }
    }

    private fun setupUi(context: Context) {
        binding.contentWrap.clipToOutline = true
        binding.videoWrap.clipToOutline = true

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack(R.id.CCTVDetailFragment, true)
            this.findNavController().navigate(R.id.action_CCTVMapFragment_to_CCTVDetailFragment)
        }
        binding.mFullScreens.setOnClickListener {
            mCCTVViewModel.fullScreen(!mExoPlayerFullscreen)
        }
        binding.tvTitle.text = getString(R.string.cctv_video_from_date, chosenDate.format(mDateFormatter))
        binding.rvTimeFragmentButtons.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.HORIZONTAL
                ).apply {
                    setDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.divider_empty
                        ) as Drawable
                    )
                }
            )
            adapter = TimeFragmentButtonsAdapter(
                chosenDate, mCCTVViewModel.availableRanges,
                {
                    mViewModel.changePlayerInterval(it)
                },
                {
                    mViewModel.positionItemRecyclerViewInterval(it)
                }
            )
            (adapter as TimeFragmentButtonsAdapter).select(
                mViewModel.positionRecyclerViewInterval.value ?: 0
            )
            if ((adapter as TimeFragmentButtonsAdapter).itemCount == 0) {
                binding.btnMainAction.visibility = View.INVISIBLE
            } else {
                binding.btnMainAction.visibility = View.VISIBLE
            }
        }
        binding.btnPlay.setOnClickListener {
            mViewModel.toggleVideoPlay()
        }
        binding.tvSpeedDown.setOnClickListener {
            mViewModel.changeSpeed(0.5)
        }
        binding.tvSpeedUp.setOnClickListener {
            mViewModel.changeSpeed(2.0)
        }
        binding.btnMainAction.setOnClickListener {
            mViewModel.pressMainButton(context)
        }
        binding.btnToPlayMode.setOnClickListener {
            mViewModel.changeUIMode(CCTVTrimmerViewModel.UiMode.Play)
        }
        binding.btnStepPlus.setOnClickListener {
            binding.rangeTrimmer.slider.interval?.let {
                mViewModel.changeTrimmerIntervalTo(15 * 60, it)
            }
        }
        binding.btnStepMinus.setOnClickListener {
            binding.rangeTrimmer.slider.interval?.let {
                mViewModel.changeTrimmerIntervalTo(-15 * 60, it)
            }
        }
        binding.rangePlayer.slider.run {
            setCurrentDate(chosenDate)
            setSeekChangeListener { progress ->
                playerSeekTo((playerDuration() * progress).toLong())
                mViewModel.savePlayerState(playerCurrentPosition())
            }
        }
        binding.rangeTrimmer.slider.run {
            setCurrentDate(chosenDate)
            setSelectionChangeListener { from, to ->
                mViewModel.saveCurrentSelection(TimeInterval(from, to, DataModule.serverTz))
            }
            setTrimMoveListener { progress ->
                interval?.let {
                    mViewModel.downloadThumbAt(progress, it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("__Q__   createPlayer from onStart")
        mPlayer = createPlayer(mCCTVViewModel.chosenCamera.value?.serverType, binding.mPlayerView)
        mCurrentPlaybackData?.run {
            changeVideoSource(this)
        }
    }

    override fun onStop() {
        super.onStop()

        mViewModel.stopVideoPlay()
        Timber.d("__Q__   releasePlayer from onStop")
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()

        if (!isHidden) {
            mViewModel.restoreSeek()
            if (mExoPlayerView == null) {
                mExoPlayerView = view?.findViewById(R.id.mPlayerView)
            }
        }
    }

    private fun setupObserve(context: Context) {
        mViewModel.changePlayerInterval.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.rangePlayer.slider.setIntervalPlayer(it.interval)
                changeVideoSource(it)
            }
        )

        mViewModel.changeTrimInterval.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.rangeTrimmer.slider.setIntervalTrimmer(it.interval, it.reset)
                generateTrimmerRanges(it.interval)
            }
        )

        mViewModel.shiftPickerPosition.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.rangeTrimmer.slider.shiftPickerPositionByMs(it)
            }
        )

        mViewModel.playState.observe(
            viewLifecycleOwner
        ) { playing ->
            binding.btnPlay.isSelected = playing
            mPlayer?.playWhenReady = playing
        }

        mViewModel.playSpeed.observe(
            viewLifecycleOwner
        ) { num ->
            if (num == MAX_SPEED_UP_VALUE) {
                binding.tvSpeedUp.text = ""
            } else {
                val speedUpText = (num * 2).clamp(0.25, 32.0)
                    .toString().removeTrailingZeros()
                binding.tvSpeedUp.text =
                    getString(
                        R.string.cctv_speed_template,
                        speedUpText
                    )
            }
            if (num == MIN_SPEED_DOWN_VALUE) {
                binding.tvSpeedDown.text = ""
            } else {
                val speedDownText = (num * 0.5).clamp(0.125, 8.0)
                    .toString().removeTrailingZeros()
                binding.tvSpeedDown.text =
                    getString(
                        R.string.cctv_speed_template,
                        speedDownText
                    )
            }
            mPlayer?.setPlaybackSpeed(num.toFloat())
        }

        mViewModel.videoLoaderVisible.observe(
            viewLifecycleOwner,
            EventObserver { visible ->
                binding.mVideoLoader.show(visible)
            }
        )

        mViewModel.uiMode.observe(
            viewLifecycleOwner
        ) { mode ->
            mode?.run {
                when (this) {
                    CCTVTrimmerViewModel.UiMode.Trim -> setTrimMode(true)
                    CCTVTrimmerViewModel.UiMode.Play -> setTrimMode(false)
                }
            }
        }

        mViewModel.restoreSeek.observe(
            viewLifecycleOwner,
            EventObserver { position ->
                binding.rangePlayer.slider.setSeekFromPlayer(position)
                playerSeekTo(position)
                mPlayer?.playWhenReady = (mViewModel.playState.value ?: false)
            }
        )

        mViewModel.updateSeekTick.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.rangePlayer.slider.setSeekFromPlayer(playerCurrentPosition())
                mViewModel.savePlayerState(playerCurrentPosition())
            }
        )

        mViewModel.alertEvent.observe(
            viewLifecycleOwner,
            EventObserver { type ->
                if (type == dialogPrepareVideo) {
                    showStandardAlert(
                        context,
                        R.string.cctv_dialog_video_prepare_title,
                        R.string.cctv_dialog_video_prepare_message,
                        R.string.cctv_dialog_video_prepare_button
                    ) {
                    }
                }
            }
        )

        mViewModel.playerMaskImages.observe(
            viewLifecycleOwner,
            EventObserver { images ->
                binding.rangePlayer.setMaskImages(images)
            }
        )

        mViewModel.trimmerMaskImages.observe(
            viewLifecycleOwner,
            EventObserver { images ->
                binding.rangeTrimmer.setMaskImages(images)
            }
        )

        mViewModel.trimmerPreviewImage.observe(
            viewLifecycleOwner
        ) { image ->
            view?.findViewById<ImageView>(R.id.mPreview)?.setImageBitmap(image)
        }

        mCCTVViewModel.stateFullScreen.observe(
            viewLifecycleOwner
        ) {
            mExoPlayerFullscreen = it
            if (it) {
                setFullscreenMode()
            } else {
                setNormalMode()
            }
        }

        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) {
            it?.let { cctvData ->
                val initialThumb = mCCTVViewModel.initialThumb
                mViewModel.initialize(cctvData, initialThumb)
                mCurrentPlaybackData?.run {
                    Timber.d("__Q__   releasePlayer from chosenCamera observer")
                    releasePlayer()
                    Timber.d("__Q__   createPlayer from chosenCamera observer")
                    createPlayer(cctvData.serverType, binding.mPlayerView)
                    changeVideoSource(this)
                }
            }
        }
    }

    private fun setTrimMode(active: Boolean) {
        view?.findViewById<ImageView>(R.id.mPreview)?.show(active)
        binding.panelTrim.show(active)
        binding.panelPlay.show(!active)
        binding.btnMainAction.setText(if (active) R.string.cctv_download_and_get_link else R.string.cctv_choose_fragment)
        binding.mFullScreens.isVisible = !active
        binding.rangePlayer.show(!active, true)
        binding.rangeTrimmer.show(active, true)
        binding.rvTimeFragmentButtonsWrap.show(!active)
    }

    private fun createPlayer(
        serverType: MediaServerType?,
        videoView: PlayerView
    ): BaseCCTVPlayer {
        Timber.d("__Q__ createPlayer()")

        val callbacks = object : BaseCCTVPlayer.Callbacks {
            override fun onPlayerStateReady() {
                canRenewToken = true
                (mPlayer as? DefaultCCTVPlayer)?.getPlayer()?.videoFormat?.let {
                    if (it.width > 0 && it.height > 0) {
                        (binding.mPlayerView.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
                    }
                }
                if (mPlayer?.playWhenReady == true) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                mViewModel.changePlaybackState(Player.STATE_READY)
            }

            override fun onPlayerStateEnded() {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (currentArchiveRangeIndex < archiveRanges.size - 1) {
                    ++currentArchiveRangeIndex
                    prepareMedia(0L, mPlayer?.playWhenReady == true)
                }
                mViewModel.changePlaybackState(Player.STATE_ENDED)
            }

            override fun onPlayerStateBuffering() {
                mViewModel.changePlaybackState(Player.STATE_BUFFERING)
            }

            override fun onPlayerStateIdle() {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                mViewModel.changePlaybackState(Player.STATE_IDLE)
            }

            override fun onPlayerError(exception: Exception) {
                mViewModel.showVideoLoader(false)

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

                            mViewModel.stopVideoPlay()
                            Timber.d("__Q__   releasePlayer from onPlayerError")
                            releasePlayer()
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            Timber.d("__Q__   createPlayer from onPlayerError")
                            mPlayer = createPlayer(mCCTVViewModel.chosenCamera.value?.serverType, binding.mPlayerView)
                            mCurrentPlaybackData?.run {
                                changeVideoSource(this)
                            }
                            mViewModel.restoreSeek()
                            if (mExoPlayerView == null) {
                                mExoPlayerView = view?.findViewById(R.id.mPlayerView)
                            }
                        }
                    }

                    return
                }

                mCCTVViewModel.showGlobalError(exception)
            }
        }

        val player = if (serverType == MediaServerType.MACROSCOP) MacroscopPlayer(requireContext(), forceVideoTrack, callbacks) else DefaultCCTVPlayer(requireContext(), forceVideoTrack, callbacks)
        videoView.player = player.getPlayer()
        videoView.useController = false

        val p = videoView.parent as ViewGroup
        p.removeView(videoView)
        p.addView(videoView, 0)

        binding.zlArchive.setSingleTapConfirmedListener {
            if (mExoPlayerFullscreen) {
                if (areVideoControllersShown) {
                    hideVideoControllers()
                } else {
                    showVideoControllers()
                }
            }
        }

        //двойной тап делает перемотку вперед или назад в зависимости от места двойного тапа: слева - назад, справа - вперед
        binding.zlArchive.setDoubleTapConfirmedListener { x_pos ->
            if (mPlayer?.isReady() == true && x_pos != null) {
                var currentPosition = playerCurrentPosition()
                var seekStep = CCTVTrimmerViewModel.SEEK_STEP
                if (x_pos.toInt() < binding.zlArchive.width / 2) {
                    seekStep = -seekStep
                }
                currentPosition += seekStep
                if (currentPosition > playerDuration()) {
                    currentPosition = playerDuration() - 1
                }
                playerSeekTo(currentPosition)

                val ivAnimation: ImageView? = if (seekStep < 0) {
                    view?.findViewById(R.id.ivBackwardArchive)
                } else {
                    view?.findViewById(R.id.ivForwardArchive)
                }

                //делаем анимацию значка перемотки
                animationFadeInFadeOut(ivAnimation)
            }
        }

        return player
    }

    private fun prepareMedia(seekMediaTo: Long = 0, doPlay: Boolean = false) {
        mViewModel.showVideoLoader(true)
        try {
            val hls = mCCTVViewModel.chosenCamera.value?.getHlsAt(archiveRanges[currentArchiveRangeIndex].from, archiveRanges[currentArchiveRangeIndex].durationSeconds, DataModule.serverTz)
            mPlayer?.prepareMedia(hls, archiveRanges[currentArchiveRangeIndex].fromUtc, archiveRanges[currentArchiveRangeIndex].durationSeconds, seekMediaTo, doPlay)
        } catch (e: Throwable) {
            mViewModel.showVideoLoader(false)
            mViewModel.handleError(e)
        }
    }

    private fun changeVideoSource(
        data: CCTVTrimmerViewModel.PlayerIntervalChangeData
    ) {
        mCurrentPlaybackData = data

        //создаем список проигрываемых интервалов
        generateArchiveRanges()
        currentArchiveRangeIndex = 0

        prepareMedia(0L, mPlayer?.playWhenReady ?: false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.rangePlayer.setupRV(requireContext())
        (binding.mPlayerView.parent as ZoomLayout).resetZoom()
    }

    private fun releasePlayer() {
        Timber.d("__Q__ call releasePlayer()")
        mPlayer?.releasePlayer()
        mPlayer = null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __onHiddenChanged hidden = $hidden")

        if (hidden) {
            mViewModel.stopVideoPlay()
            Timber.d("__Q__   releasePlayer from onHiddenChanged")
            releasePlayer()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            Timber.d("__Q__   createPlayer from onHiddenChanged")
            mPlayer = createPlayer(mCCTVViewModel.chosenCamera.value?.serverType, binding.mPlayerView)
            mCurrentPlaybackData?.run {
                changeVideoSource(this)
            }
            mViewModel.restoreSeek()
            if (mExoPlayerView == null) {
                mExoPlayerView = view?.findViewById(R.id.mPlayerView)
            }
        }

        super.onHiddenChanged(hidden)
    }
}
