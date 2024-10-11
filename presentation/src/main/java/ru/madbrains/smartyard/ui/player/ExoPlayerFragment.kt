package ru.madbrains.smartyard.ui.player

import EvenItemDecoration
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.domain.model.response.Plog
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentExoPlayerBinding
import ru.madbrains.smartyard.ui.getStatusBarHeight
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.cctv_video.ZoomLayout
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateFormat
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateTimeFormat
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.minuteWithSecondsFormat
import ru.madbrains.smartyard.ui.player.classes.CustomHorizontalLayoutManager
import ru.madbrains.smartyard.ui.player.classes.DatePicker
import ru.madbrains.smartyard.ui.player.classes.ScaleGestureListener
import ru.madbrains.smartyard.ui.player.classes.TimerWatcher
import ru.madbrains.smartyard.ui.player.classes.VideoFrameLoader
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class ExoPlayerFragment : Fragment() {
    lateinit var binding: FragmentExoPlayerBinding
    private val mExoPlayerViewModel: ExoPlayerViewModel by sharedViewModel()

    private lateinit var timerWatcher: TimerWatcher
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private lateinit var eventItemDecoration: EvenItemDecoration
    private lateinit var calendar: DatePicker
    private var rv: RecyclerView? = null
    private var player: ExoPlayer? = null

    private var url = ""
    private val timer = Timer()
    private var lineTimer: Timer? = null
    private var isOnline = true

    private var isTouch = false
    private val mapIndex = mutableMapOf<Int, Plog>()

    ////////////////////////
    private var offsetY: Float = 0f
    private var initialTouchY: Float = 0f
    private var parentContainer: ViewGroup? = null

    ////////////////////////
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduler()
        (activity as? MainActivity)?.hideSystemUI()
        timerWatcher = TimerWatcher(binding)
        binding.clFragmentExoPlayer.setPadding(0, getStatusBarHeight(context, true), 0, 0)
        calendar = DatePicker(requireContext(), mExoPlayerViewModel)
        rv = binding.rvTimeLine
        rv?.let {
            scaleGestureDetector = ScaleGestureDetector(
                requireContext(),
                ScaleGestureListener(mExoPlayerViewModel, it, requireContext())
            )
        }
        binding.tvCalendar.text = dateFormat.format(mExoPlayerViewModel.calendar.value)
        parentContainer = view.parent as ViewGroup

        mExoPlayerViewModel.getPlog(mExoPlayerViewModel.timeInTimer.value!!)
        initPlayer()
        setupAdapter()
        observer()
        clickListeners()

//        setParentContainerPadding()
    }

    private fun setParentContainerPadding() {
        val paddingTop = if (parentContainer?.paddingTop == 0) {
            getStatusBarHeight(context)
        } else {
            0
        }
        parentContainer?.setPadding(0, paddingTop, 0, 0)
    }

    private fun scheduler() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (url.isNotEmpty() && !VideoFrameLoader.isLoading && isTouch) {
                    VideoFrameLoader.loadVideoFrame(context, url, binding.ivPreviewImg)
                    url = ""
                }
            }
        }, 0, 200)
    }

    private fun schedulerTimeLine(period: Long = 500) {
        if (lineTimer == null) {
            lineTimer = Timer()
            val handler = Handler(Looper.getMainLooper())
            var offset = 0f
            var time = 0f
            var oldTime = 0f
            var timeInTimer = 0L
            var playerTime = 0L
            var oldPlayerTime = 0L
            lineTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    handler.post {
                        offset = getOffset()
                        time = getPlayerTime().times(offset)
                        if (time.toInt() > 0 && oldTime.toInt() == 0) {
                            oldTime = time - offset
                        }
                        if (time.toInt() > oldTime.toInt()) {
                            rv?.smoothScrollBy(0, (time.toInt() - oldTime.toInt()) * -1)
                            timeInTimer = mExoPlayerViewModel.timeInTimer.value!!
                            oldTime = time
                        } else {
                            //TODO Не доделал
//                            val timeText = minuteWithSecondsFormat.format(binding.tvTimerBadge.text ?: 0).toString()
//                            val timeIterable = minuteWithSecondsFormat.parse(timeInTimer.toString()).time
                            if (timeInTimer == 0L) {
                                timeInTimer = mExoPlayerViewModel.timeInTimer.value!!
                            }
//                            timeInTimer = if(timeInTimer == 0L) mExoPlayerViewModel.timeInTimer.value!! else timeInTimer
                            val currentTime = timeInTimer.plus(period)
                            timeInTimer = currentTime
//                            binding.tvArchiveTitle.text = minuteWithSecondsFormat.format(currentTime)
                        }
                    }
                }
            }, 0, period)
        }
    }

    private fun getPlayerTime() = player?.currentPosition?.div(1000)?.toInt() ?: 0

    private fun getOffset(): Float = with(mExoPlayerViewModel) {
        val height = scaleHeight.value
        val scale = scale.value!!
        val oneMinuteInSeconds = 60
        var offset = 0f
        if (height != null) {
            val heightInOneMinute = height.toFloat() / scale
            offset = heightInOneMinute / oneMinuteInSeconds
        }
        return offset
    }


    //TODO GO TO ONLINE TEST VERSION
    private fun goToOnline() = with(mExoPlayerViewModel) {
        if (chosenIndex.value != null) {
            if (timeInTimer.value!! > getTimeLineItem().value!!.rangeTime[1].entries.first().key) {
                isOnline = true
                binding.tvTimerBadge.text = "ОНЛАЙН"
//                binding.clImgPreview.visibility = View.INVISIBLE
//                binding.clImgPreview.elevation = 0F
            } else {
                isOnline = false
            }
        }
    }

    private fun playOnlineHls() = with(mExoPlayerViewModel) {
        chosenIndex.value?.let {
            changeVideoSource(cameraList.value?.get(it)?.hls ?: "")
            rv?.scrollToPosition(0)
        }
    }

    private fun clickListeners() {
        with(binding) {
            cvTablet.setOnTouchListener { _, event ->
//                (binding.playerView.parent as ZoomLayout).resetZoom()
                handleTouch(event)
            }

            tvCalendar.setOnClickListener {
                createDatePicker()?.show()
            }
            ibVolume.setOnClickListener { muteVideo() }

            tvGoToOnline.setOnClickListener {
                playOnlineHls()
            }
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val height = binding.arPlayer.height
        val windowView = binding.clFragmentExoPlayer
        val layoutParams = windowView.layoutParams as ConstraintLayout.LayoutParams
        val lp = binding.arPlayer.layoutParams as ConstraintLayout.LayoutParams

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                offsetY = event.rawY - windowView.y
                initialTouchY = event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                val newTopMargin = (event.rawY - offsetY).toInt()
                val heightRv = (binding.rvListItem.height * -1)

                if (newTopMargin in heightRv..0) {
                    layoutParams.topMargin = newTopMargin
                    windowView.layoutParams = layoutParams

                    lp.height = height
                    binding.clPlayerCameras.alpha = offsetY
                    binding.arPlayer.layoutParams = lp
                }
            }

            MotionEvent.ACTION_UP -> {
                val heightRv = (binding.rvListItem.height * -1)
                val deltaY = event.rawY - initialTouchY
                if (deltaY > 0) {
                    layoutParams.topMargin = 0
                } else {
                    layoutParams.topMargin = heightRv
                }
                windowView.layoutParams = layoutParams
            }
        }
        return true
    }

    private fun muteVideo() {
        val imgVolumeOff =
            ResourcesCompat.getDrawable(resources, R.drawable.baseline_volume_off_24, null)
        val imgVolumeUp =
            ResourcesCompat.getDrawable(resources, R.drawable.baseline_volume_up_24, null)
        if (player?.volume == 0f) {
            player?.volume = 1f
            binding.ibVolume.setImageDrawable(imgVolumeUp)
        } else {
            player?.volume = 0f
            binding.ibVolume.setImageDrawable(imgVolumeOff)
        }
    }

    private fun createDatePicker(): DatePickerDialog? {
        val dateConvert = mExoPlayerViewModel.getTimeLineItem().value!!.rangeTime
        val pastDateSplit = dateConvert[dateConvert.size - 1].entries.first().key
        val presentDateSplit = dateConvert[0].entries.first().key
        return calendar.createDatePickerDialog(pastDateSplit, presentDateSplit)
        { scrollToPosition() }
    }

    private fun initPlayer() {
        if (player == null && view != null) {
            binding.arPlayer.setAspectRatio(16F / 9F)
            createPlayer(binding.playerView)

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun scaleListener() {
        binding.rvTimeLine.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun reloadItemDecorator() {
        rv?.removeItemDecoration(eventItemDecoration)
        rv?.addItemDecoration(eventItemDecoration)
    }

    private fun setEventImg(): MutableMap<Int, Plog> {
        try {
            val mapIndex = mutableMapOf<Int, Plog>()
//            val scale = (mExoPlayerViewModel.SCALE * 60) * 1000
            val scale = (mExoPlayerViewModel.scale.value!! * 60) * 1000
            val range = mExoPlayerViewModel.getTimeLineItem().value?.rangeTime
            if (range != null) {
                mExoPlayerViewModel.eventsCamera.value?.forEach {
                    it.event?.forEach { plog ->
                        val plogDate = dateTimeFormat.parse(plog._date).time
                        range.forEachIndexed { index, _ ->
                            if (plogDate < range[index].entries.first().key && plogDate > range[index].entries.first().key - scale) {
                                mapIndex[index] = plog
                            }
                        }
                    }
                }
            }
            return mapIndex
        } catch (e: Exception) {
            Timber.e(e, "ExoPlayerFragment setEventImg() ERROR")
            return mutableMapOf()
        }
    }


    private fun setImgInEvent(imageUrl: String, firstVisibleView: View, index: Int) {
        val eventImageView = firstVisibleView.findViewById<ImageView>(R.id.iv_event_img_flex)
        val eventType = firstVisibleView.findViewById<ImageView>(R.id.iv_event_type)
        val containerEvent = firstVisibleView.findViewById<ConstraintLayout>(R.id.clBoxEvent)
        val clLines = firstVisibleView.findViewById<ConstraintLayout>(R.id.cl_lines)
        val heightInOneSecond =
            (clLines?.height!!.toFloat() / mExoPlayerViewModel.scale.value!!) / 60
        val timeLineItem = mExoPlayerViewModel.getTimeLineItem().value
        if (timeLineItem != null) {
            mapIndex[index].let {
                if (it != null) {
                    val timeEvent = dateTimeFormat.parse(it._date).time
                    val cellTime = timeLineItem.rangeTime[index].entries.first().key
                    val seconds = (cellTime - timeEvent) / 1000
                    val y = (heightInOneSecond * seconds)
                    containerEvent?.y = y

                    val heightImage = eventImageView.height
                    val timeFactor = (((heightImage / heightInOneSecond).toLong() / 2) * 1000)
                    val timeInTimer = mExoPlayerViewModel.timeInTimer.value!!
                    eventType.setImageDrawable(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            getEventTypeResource(it.eventType)
                        )
                    )
                    if (timeInTimer in timeEvent - timeFactor..timeEvent + timeFactor) {
                        if (eventImageView.animation == null) {
                            imageScaleAnimation(firstVisibleView)
                        }
                    } else {
                        if (eventImageView.animation != null) {
                            endImageScaleAnimation(firstVisibleView)
                        }
                    }
                }
            }
        }
        VideoFrameLoader.loadVideoFrame(
            requireContext(),
            imageUrl,
            eventImageView
        )
    }

    private fun getTimeInTimeLine(rangeTimeItem: Long, firstVisibleView: View): Long {
        return timerWatcher.getTime(
            firstVisibleView,
            rangeTimeItem,
            mExoPlayerViewModel.scale.value!!
        )
    }

    private fun getEventTypeResource(type: Int): Int {
        return when (type) {
            Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> R.drawable.ic_event_no_answer_call
            Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> R.drawable.ic_event_answer_call
            Plog.EVENT_OPEN_BY_KEY -> R.drawable.ic_event_open_by_key
            Plog.EVENT_OPEN_FROM_APP -> R.drawable.ic_event_open_by_app
            Plog.EVENT_OPEN_BY_FACE -> R.drawable.ic_event_open_by_face
            Plog.EVENT_OPEN_BY_CODE -> R.drawable.ic_event_open_by_code
            Plog.EVENT_OPEN_GATES_BY_CALL -> R.drawable.ic_el_gates
            Plog.EVENT_OPEN_BY_LINK -> R.drawable.ic_el_app
            else -> android.R.color.transparent
        }
    }

    private fun imageScaleAnimation(findView: View) {
        val animScaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
        val eventImageView = findView.findViewById<ImageView>(R.id.iv_event_img_flex)
        animScaleUp.fillAfter = true
        eventImageView.startAnimation(animScaleUp)
    }

    private fun endImageScaleAnimation(findView: View) {
        val animScaleDown = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_down)
        val eventImageView = findView.findViewById<ImageView>(R.id.iv_event_img_flex)
        eventImageView.startAnimation(animScaleDown)
    }

    private fun scrollStateChanged() {
//        val observer = recyclerView.viewTreeObserver
//        eventItemDecoration = EvenItemDecoration(requireContext(), mExoPlayerViewModel)
//        rv.addItemDecoration(eventItemDecoration)
        ////////////
        var timeInLine: Long
        rv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Вызывается при каждом изменении прокрутки
                val isScale = (rv?.layoutManager as CustomHorizontalLayoutManager).isScale

                val countItemsRv = recyclerView.childCount
                val firstVisibleItem =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val firstVisibleView =
                    recyclerView.layoutManager?.findViewByPosition(firstVisibleItem)
                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val lastVisibleView =
                    (recyclerView.layoutManager as LinearLayoutManager).findViewByPosition(
                        lastVisibleItemPosition
                    )

                if (firstVisibleView != null && !isScale) {
                    goToOnline()

                    val rangeTime =
                        mExoPlayerViewModel.getTimeLineItem().value?.rangeTime?.get(firstVisibleItem)
                    if (rangeTime != null) {
                        val rangeTimeItem = rangeTime.entries.first().key
                        timeInLine = getTimeInTimeLine(rangeTimeItem, firstVisibleView)
                        mExoPlayerViewModel.setTimeInTimer(timeInLine)

                        val timeInLineShr = timeInLine / 1000
                        url = with(mExoPlayerViewModel) {
                            "${cameraList.value!![chosenIndex.value!!].url}/$timeInLineShr-preview.mp4?token=${cameraList.value!![0].token}"
                        } //TODO Need Refactor


                        // Установка картинки в событие
                        for (i in firstVisibleItem..lastVisibleItemPosition) {
                            val findView = recyclerView.layoutManager?.findViewByPosition(i)
                            val imageUrl = mapIndex[i]?.preview
                            val boxEvent = findView?.findViewById<ConstraintLayout>(R.id.clBoxEvent)
                            if (imageUrl != null && findView != null) {
                                Timber.d("IMAGEURLEVENTEPT mapindex ${mapIndex[i]?.mechanizmaDescription} URL $imageUrl")
                                boxEvent?.visibility = View.VISIBLE
                                setImgInEvent(imageUrl, findView, i)
                            } else {
                                boxEvent?.visibility = View.GONE
                            }
                        }

                        try {
                            val timeForBadge = minuteWithSecondsFormat.format(Date(timeInLine))
                            if (binding.tvTimerBadge.text != timeForBadge && !isOnline) {
                                binding.tvTimerBadge.text = timeForBadge
                            }
                            val calendarDate = mExoPlayerViewModel.calendar.value!!
                            if (dateFormat.format(calendarDate) != dateFormat.format(rangeTimeItem)) {
                                mExoPlayerViewModel.setCalendar(rangeTimeItem)
                                binding.tvCalendar.text = dateFormat.format(rangeTimeItem)

                                mExoPlayerViewModel.getPlog(mExoPlayerViewModel.timeInTimer.value!!)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "scrollStateChanged() ERROR")
                        }
                    }
                } else { // Обработка случая, когда элемент еще не создан
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val firstVisibleItem =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val firstVisibleView =
                    recyclerView.layoutManager?.findViewByPosition(firstVisibleItem)

                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // Прокрутка остановлена
                        if (isTouch) {
                            isTouch = false
                            archivePlayHls()
                        }
                    }

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // Пользователь проводит прокрутку
                        isTouch = true
                        stopArchiveVideo()
                    }

                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // Прокрутка "устанавливается" (происходит анимация прокрутки)
                    }
                }
            }
        })
        scaleListener()
    }

    private fun playArchiveVideo() {
        if (!isTouch) {
            schedulerTimeLine()
            binding.clImgPreview.visibility = View.INVISIBLE
            binding.clImgPreview.elevation = 0F
        }
    }

    private fun stopArchiveVideo() {
        stopSchedulers()
        binding.clImgPreview.visibility = View.VISIBLE
        binding.clImgPreview.elevation = 1F

    }

    private fun stopSchedulers() {
        lineTimer?.cancel()
        lineTimer = null
    }

    private fun archivePlayHls() = with(mExoPlayerViewModel) {
        val chosenIndex = chosenIndex.value ?: 0
        val duration = timeInTimer.value?.div(1000L)
        val url = duration?.let { cameraList.value?.get(chosenIndex)?.archiveHls(it) } ?: ""
        changeVideoSource(url)
    }

    private fun scrollToPosition() {
        val rangeTime = mExoPlayerViewModel.getTimeLineItem().value?.rangeTime
        val calendarDate = dateFormat.format(mExoPlayerViewModel.calendar.value)
        var position = 0
        for (i in rangeTime?.indices!!) {
            if (dateFormat.format(rangeTime[i].entries.first().key)
                    .contains(calendarDate)
            ) {
                position = if (i == 0) 0 else i - 1
                break
            }
        }
        binding.rvTimeLine.scrollToPosition(position)
    }

    private fun observer() {
        mExoPlayerViewModel.chosenIndex.observe(
            viewLifecycleOwner,
            Observer {
                try {
                    if (it != null) {
                        binding.tvArchiveTitle.text =
                            mExoPlayerViewModel.cameraList.value?.get(it)?.name
                        changeVideoSource(mExoPlayerViewModel.cameraList.value?.get(it)?.hls ?: "")
                        mExoPlayerViewModel.loadPeriod { reloadTimeLineAdapter() }
                        binding.ivPreviewImg.setImageDrawable(null)
                        releaseVariables()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Observer chosenIndex Exception")
                }
            }
        )
        mExoPlayerViewModel.eventsCamera.observe(
            viewLifecycleOwner
        ) {
            getEventsMap()
        }

        mExoPlayerViewModel.scale.observe(
            viewLifecycleOwner
        ) {
            if (binding.rvTimeLine.adapter is TimeLineArchiveAdapter) {
                (binding.rvTimeLine.adapter as TimeLineArchiveAdapter).updateData(
                    mExoPlayerViewModel.getTimeLineItem().value!!,
                    it
                )
            }
        }

        mExoPlayerViewModel.scaleHeight.observe(
            viewLifecycleOwner
        ) {
            if (binding.rvTimeLine.adapter is TimeLineArchiveAdapter) {
                val adapter = (binding.rvTimeLine.adapter as TimeLineArchiveAdapter)
                adapter.scaleHeight = it
                adapter.SCALE = mExoPlayerViewModel.scale.value!!
            }
        }
    }

    private fun getEventsMap() {
        val event = setEventImg()
        if (event.isNotEmpty())
            event.forEach { (k, v) ->
                mapIndex[k] = v
            }
    }

    private fun changeVideoSource(url: String) {
        if (url.isNotEmpty()) {
            player.let { player ->
                player?.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                player?.prepare()
//                (binding.playerView.parent as ZoomLayout).resetZoom()
            }
        }
    }

    private fun setupAdapter() {
        if (!mExoPlayerViewModel.cameraList.value.isNullOrEmpty()) {
            val adapter =
                ExoPlayerTabsAdapter(mExoPlayerViewModel.cameraList.value!!){
                    mExoPlayerViewModel.chooseCamera(it)
                }
            binding.rvListItem.adapter = adapter
        }
    }

    private fun reloadTimeLineAdapter() {
        if (!mExoPlayerViewModel.cameraList.value.isNullOrEmpty()) {
            val list = mExoPlayerViewModel.getTimeLineItem().value
            if (list != null) {
                binding.rvTimeLine.adapter = null
//                val lineAdapter = TimeLineAdapter(mExoPlayerViewModel, requireContext())
                val lineAdapter = TimeLineArchiveAdapter(requireContext()) { a, b ->
                    if (a != null) {
                    }
                }
                binding.rvTimeLine.adapter = lineAdapter
                lineAdapter.updateData(list, mExoPlayerViewModel.scale.value!!)
                binding.rvTimeLine.isNestedScrollingEnabled = false
                binding.rvTimeLine.setHasFixedSize(true)
                scrollStateChanged()
            }
        }
    }

    private fun createPlayer(playerView: PlayerView) {
        player = ExoPlayer.Builder(this@ExoPlayerFragment.requireContext()).build()
        playerView.player = player
        playerView.useController = false
        player?.playWhenReady = true
        muteVideo()

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                binding.pbProgressBar.isVisible = !isPlaying
                if (isPlaying) {
                    playArchiveVideo()
                } else {
                    stopSchedulers()
                }
            }

            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                super.onSurfaceSizeChanged(width, height)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                try {
                    Timber.d("EROORPLAYER onPlayerError")
                    player?.prepare()
                    player?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (binding.playerView.parent as ZoomLayout).resetZoom()
    }

    override fun onResume() {
        super.onResume()
        try {
            player?.prepare()
            player?.play()
        } catch (e: PlaybackException) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.stop()
        stopSchedulers()
    }

    override fun onDestroy() {
        (activity as? MainActivity)?.showSystemUI()
//        setParentContainerPadding()
        releasePlayer()
        stopSchedulers()
        releaseVariables()
        super.onDestroy()
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun releaseVariables() {
        mapIndex.clear()
        url = ""
    }

    companion object {
        const val SCALE_X1 = 15
        const val SCALE_X2 = 2
        const val HEIGHT_TIME_SECTOR = 330
        const val SCALE_FACTOR = HEIGHT_TIME_SECTOR * 3
    }
}