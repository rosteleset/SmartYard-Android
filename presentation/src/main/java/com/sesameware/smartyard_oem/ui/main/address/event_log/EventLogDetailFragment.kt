package com.sesameware.smartyard_oem.ui.main.address.event_log

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.Px
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.video.VideoListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.domain.model.response.Plog
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.event_log.adapters.EventLogDetailAdapter
import timber.log.Timber
import org.threeten.bp.DateTimeUtils
import com.sesameware.smartyard_oem.databinding.FragmentEventLogDetailBinding
import com.sesameware.smartyard_oem.ui.animationFadeInFadeOut
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.MyGestureDetector
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.dialogAddPhoto.DialogAddPhotoFragment
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.dialogRemovePhoto.DialogRemovePhotoFragment
import com.sesameware.smartyard_oem.ui.webview_dialog.WebViewDialogFragment

class EventLogDetailFragment : Fragment() {
    private var _binding: FragmentEventLogDetailBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by sharedViewModel<EventLogViewModel>()
    private var snapHelper: PagerSnapHelper? = null

    private var prevLoadedIndex = -1
    private var mPlayer: SimpleExoPlayer? = null
    private var mPlayerView: PlayerView? = null
    private var videoUrl = ""
    private var savedPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentEventLogDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()
        binding.ivEventLogDetailBack.setOnClickListener {
            releasePlayer()
            this.findNavController().popBackStack()
        }
    }

    private fun initPlayer() {
        if (mPlayer == null) {
            val trackSelector = DefaultTrackSelector(requireContext())
            mPlayer = SimpleExoPlayer.Builder(requireContext())
                .setTrackSelector(trackSelector)
                .build()
            mPlayer?.playWhenReady = true
            mPlayer?.addListener(object : Player.EventListener{
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    if (state == Player.STATE_ENDED) {
                        mPlayer?.playWhenReady = false
                        mPlayer?.seekTo((mPlayer?.duration ?: 0) - 1)
                        Toast.makeText(requireContext(), EventLogViewModel.PAUSE, Toast.LENGTH_LONG).show()
                    }
                }
            })
            mPlayer?.addVideoListener(object : VideoListener {
                override fun onRenderedFirstFrame() {
                    super.onRenderedFirstFrame()
                    mPlayerView?.alpha = 1.0f
                }

                //этот метод нужен для установки нужной высоты вьюшки с видео
                @Deprecated("Deprecated in Java")
                override fun onVideoSizeChanged(
                    width: Int,
                    height: Int,
                    unappliedRotationDegrees: Int,
                    pixelWidthHeightRatio: Float
                ) {
                    super.onVideoSizeChanged(
                        width,
                        height,
                        unappliedRotationDegrees,
                        pixelWidthHeightRatio
                    )
                    mPlayerView?.let {
                        if (width > 0 && height > 0) {
                            val k = it.measuredWidth.toFloat() / width.toFloat()
                            it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (height.toFloat() * k).toInt())
                        }
                    }
                }
            })
            mPlayerView?.player = mPlayer
        }
    }

    override fun onStop() {
        mPlayerView?.alpha = 0.0f
        super.onStop()
    }

    fun releasePlayer() {
        mPlayerView?.alpha = 0.0f
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
    }

    private fun playVideo(position: Int) {
        (binding.rvEventLogDetail.adapter as? EventLogDetailAdapter)?.let { adapter ->
            val (day, index) = adapter.getPlog(position)
            if (day != null && index != null) {
                adapter.eventsByDays[day]?.get(index)?.let {eventItem ->
                    mViewModel.camMapData[eventItem.objectId]?.let { data ->
                        videoUrl = data.getHlsAt(eventItem.date.minusSeconds(EventLogViewModel.EVENT_VIDEO_BACK_SECONDS),
                            EventLogViewModel.EVENT_VIDEO_DURATION_SECONDS)
                        Timber.d("__Q__  playVideo media $videoUrl  mPlayer = $mPlayer")
                        mPlayer?.setMediaItem(MediaItem.fromUri(videoUrl))
                        mPlayer?.prepare()
                        mPlayer?.play()
                        mPlayer?.seekTo(EventLogViewModel.EVENT_VIDEO_BACK_SECONDS * 1000L)
                    }
                }
            }
        }
    }

    override fun onPause() {
        releasePlayer()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if ((activity as? MainActivity)?.binding?.bottomNav?.selectedItemId == R.id.address) {
            initPlayer()
            playVideo(savedPosition)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            releasePlayer()
        } else {
            initPlayer()
            playVideo(savedPosition)
        }

        super.onHiddenChanged(hidden)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRecycler() {
        binding.rvEventLogDetail.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = EventLogDetailAdapter(listOf(), hashMapOf(),
                {
                    mViewModel.eventsByDaysFilter[it.second]?.get(it.third)?.let { plog ->
                        plog.detailX?.flags?.let { flags ->
                            if (flags.contains(Plog.FLAG_CAN_DISLIKE)) {
                                //пользователь дизлайкнул
                                val faceId = plog.detailX?.faceId?.toInt() ?: 0
                                val photoUrl = mViewModel.faceIdToUrl[faceId] ?: plog.preview
                                val dialogRemovePhoto = DialogRemovePhotoFragment(photoUrl ?: "") {
                                    mViewModel.dislike(plog.uuid)
                                    flags.remove(Plog.FLAG_CAN_DISLIKE)
                                    flags.add(Plog.FLAG_CAN_LIKE)
                                    if (faceId > 0) {
                                        mViewModel.faceIdToUrl.remove(faceId)
                                    }
                                    (adapter as EventLogDetailAdapter).also { adapter ->
                                        adapter.eventsByDays[it.second]?.set(it.third, plog)
                                        Timber.d("__Q__ plog: $plog")
                                        adapter.notifyItemChanged(it.first)
                                    }
                                }
                                dialogRemovePhoto.show(requireActivity().supportFragmentManager, "")
                            } else {
                                if (flags.contains(Plog.FLAG_CAN_LIKE)) {
                                    //пользователь лайкнул
                                    val dialogAddPhoto = DialogAddPhotoFragment(
                                        plog.preview ?: "",
                                        plog.detailX?.face?.left ?: -1,
                                        plog.detailX?.face?.top ?: -1,
                                        plog.detailX?.face?.width ?: -1,
                                        plog.detailX?.face?.height ?: -1,
                                        plog.eventType == Plog.EVENT_OPEN_BY_FACE
                                    ) {
                                        mViewModel.like(plog.uuid)
                                        flags.remove(Plog.FLAG_CAN_LIKE)
                                        val faceId = plog.detailX?.faceId?.toInt() ?: 0
                                        if (faceId > 0) {
                                            flags.add(Plog.FLAG_CAN_DISLIKE)
                                        }
                                        flags.add(Plog.FLAG_LIKED)
                                        (adapter as EventLogDetailAdapter).also { adapter ->
                                            adapter.eventsByDays[it.second]?.set(it.third, plog)
                                            Timber.d("__Q__ plog: $plog")
                                            adapter.notifyItemChanged(it.first)
                                        }
                                    }
                                    dialogAddPhoto.show(requireActivity().supportFragmentManager, "")
                                }
                            }
                        }
                    }
                },
                {
                    WebViewDialogFragment(R.string.help_log_details).show(requireActivity().supportFragmentManager, "HelpLogDetails")
                }
            )

            val spacing = resources.getDimensionPixelSize(R.dimen.event_log_detail_spacing)
            addItemDecoration(LinearHorizontalSpacingDecoration(spacing))
            addItemDecoration(BoundsOffsetDecoration())
        }

        snapHelper = PagerSnapHelper()
        binding.rvEventLogDetail.attachSnapHelperWithListener(snapHelper!!, SnapOnScrollListener.ScrollBehavior.NOTIFY_ON_SCROLL_IDLE,
            object : OnSnapPositionChangeListener {
                override fun onSnapPositionChanged(prevPosition: Int, newPosition: Int) {
                    //Timber.d("__Q__ snap position changed: prev = $prevPosition;  new = $newPosition")
                    if (prevPosition != RecyclerView.NO_POSITION) {
                        val pvELDVideo: PlayerView? = binding.rvEventLogDetail.findViewHolderForAdapterPosition(prevPosition)?.itemView?.findViewById(R.id.pvELDVideo)
                        pvELDVideo?.alpha = 0.0f
                        mPlayer?.stop()
                        pvELDVideo?.player = null
                        mPlayerView = null
                    }
                    val pvELDVideo: PlayerView? = binding.rvEventLogDetail.findViewHolderForAdapterPosition(newPosition)?.itemView?.findViewById(R.id.pvELDVideo)
                    mPlayerView = pvELDVideo

                    val svELD: ScrollView? = binding.rvEventLogDetail.findViewHolderForAdapterPosition(newPosition)?.itemView?.findViewById(R.id.svELD)
                    val q = GestureDetector(requireContext(), MyGestureDetector(
                        {
                            if (mPlayer?.playbackState == Player.STATE_READY) {
                                if (mPlayer?.isPlaying == true) {
                                    mPlayer?.pause()
                                    Toast.makeText(requireContext(), EventLogViewModel.PAUSE, Toast.LENGTH_LONG).show()
                                } else {
                                    mPlayerView?.alpha = 1.0f
                                    mPlayer?.play()
                                    Toast.makeText(requireContext(), EventLogViewModel.PLAYING, Toast.LENGTH_LONG).show()
                                }
                            }
                        }, { x_pos ->  //двойной тап делает перемотку вперед или назад в зависимости от места двойного тапа: слева - назад, справа - вперед
                            if (mPlayer?.playbackState == Player.STATE_READY) {
                                if (x_pos != null && svELD != null && mPlayer != null) {
                                    var currentPosition = mPlayer?.currentPosition ?: 0
                                    val lastPosition = (mPlayer?.duration ?: 0) - 1
                                    var seekStep = EventLogViewModel.SEEK_STEP
                                    if (x_pos.toInt() < svELD.width / 2) {
                                        seekStep = -seekStep
                                    }
                                    currentPosition += seekStep
                                    if (currentPosition < 0)
                                        currentPosition = 0
                                    if (currentPosition > lastPosition)
                                        currentPosition = lastPosition 
                                    mPlayer?.seekTo(currentPosition)

                                    val ivAnimation: ImageView? = if (seekStep < 0) {
                                        binding.rvEventLogDetail.findViewHolderForAdapterPosition(newPosition)?.itemView?.findViewById(R.id.ivBackwardELD)
                                    } else {
                                        binding.rvEventLogDetail.findViewHolderForAdapterPosition(newPosition)?.itemView?.findViewById(R.id.ivForwardELD)
                                    }

                                    //делаем анимацию значка перемотки
                                    animationFadeInFadeOut(ivAnimation)
                                }
                            }
                        }, {
                            if (mPlayer?.playbackState == Player.STATE_READY) {
                                if (mPlayerView?.alpha == 0.0f) {
                                    mPlayerView?.alpha = 1.0f
                                    mPlayer?.play()
                                    Toast.makeText(requireContext(), EventLogViewModel.PLAYING, Toast.LENGTH_LONG).show()
                                } else {
                                    mPlayerView?.alpha = 0.0f
                                    mPlayer?.pause()
                                    Toast.makeText(requireContext(), EventLogViewModel.SCREENSHOT, Toast.LENGTH_LONG).show()
                                }
                            }
                        }))


                    svELD?.setOnTouchListener { _, event ->
                        q.onTouchEvent(event)
                        false
                    }

                    mPlayerView?.alpha = 0.0f
                    mPlayerView?.player = mPlayer
                    playVideo(newPosition)
                    savedPosition = newPosition
                }
            })

        binding.rvEventLogDetail.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val llm = binding.rvEventLogDetail.layoutManager as LinearLayoutManager
                val itemCount = binding.rvEventLogDetail.adapter?.itemCount ?: 0
                val itemPosition = llm.findLastCompletelyVisibleItemPosition()
                if (dx > 0 && itemPosition  == itemCount - 1
                    && (mViewModel.lastLoadedDayFilterIndex.value ?: 0) < mViewModel.eventDaysFilter.size - 1) {
                    //Timber.d("__Q__ call getMoreEvents()")
                    mViewModel.getMoreEvents()
                }
                if (itemPosition == llm.findFirstCompletelyVisibleItemPosition()
                    && itemPosition != RecyclerView.NO_POSITION) {
                    mViewModel.currentEventDayFilter = (binding.rvEventLogDetail.adapter as EventLogDetailAdapter).getPlog(itemPosition).first
                }

                if (dx != 0) {
                    mPlayer?.pause()
                }
            }
        })

        mViewModel.lastLoadedDayFilterIndex.observe(viewLifecycleOwner) { lastLoadedIndex ->
            if (prevLoadedIndex >= lastLoadedIndex) {
                prevLoadedIndex = lastLoadedIndex
                return@observe
            }

            (binding.rvEventLogDetail.adapter as EventLogDetailAdapter).also { adapter ->
                if (mViewModel.eventDaysFilter.isNotEmpty()) {
                    adapter.eventsDay =
                        mViewModel.eventDaysFilter.map { it.day }.subList(0, lastLoadedIndex + 1)
                    adapter.eventsByDays = mViewModel.eventsByDaysFilter
                    if (prevLoadedIndex < 0) {
                        adapter.notifyDataSetChanged()
                        mViewModel.currentEventItem?.let { currentItem ->
                            val p =
                                mViewModel.getEventItemCountTillDay(currentItem.first.plusDays(1)) + currentItem.second
                            (binding.rvEventLogDetail.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                                layoutManager.scrollToPosition(p)
                                binding.rvEventLogDetail.doOnPreDraw {
                                    val targetView =
                                        layoutManager.findViewByPosition(p) ?: return@doOnPreDraw
                                    val distanceToFinalSnap = snapHelper?.calculateDistanceToFinalSnap(
                                        layoutManager,
                                        targetView
                                    ) ?: return@doOnPreDraw
                                    if (p > 0) {
                                        layoutManager.scrollToPositionWithOffset(
                                            p,
                                            -distanceToFinalSnap[0]
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val day1 = mViewModel.eventDaysFilter[prevLoadedIndex].day
                        val count1 = mViewModel.getEventItemCountTillDay(day1)
                        val day2 = mViewModel.eventDaysFilter[lastLoadedIndex].day
                        val count2 = mViewModel.getEventItemCountTillDay(day2)
                        adapter.notifyItemRangeChanged(count1, count2 - count1)
                    }
                }
            }

            prevLoadedIndex = lastLoadedIndex
        }

        mViewModel.progress.observe(viewLifecycleOwner) {
            binding.pbEventLogDetail.isVisible = it
        }
    }
}

/** Works best with a [LinearLayoutManager] in [LinearLayoutManager.HORIZONTAL] orientation */
class LinearHorizontalSpacingDecoration(@Px private val innerSpacing: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        outRect.left = if (itemPosition == 0) 0 else innerSpacing / 2
        outRect.right = if (itemPosition == state.itemCount - 1) 0 else innerSpacing / 2
    }
}

/** Offset the first and last items to center them */
class BoundsOffsetDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        // It is crucial to refer to layoutParams.width (view.width is 0 at this time)!
        val itemWidth = view.layoutParams.width
        val offset = (parent.measuredWidthAndState - itemWidth) / 2

        if (itemPosition == 0) {
            outRect.left = offset
        } else if (itemPosition == state.itemCount - 1) {
            outRect.right = offset
        }
    }
}
