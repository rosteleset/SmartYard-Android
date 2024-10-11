package ru.madbrains.smartyard.ui.player.classes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.ScaleGestureDetector
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.madbrains.smartyard.ui.player.ExoPlayerFragment
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel
import ru.madbrains.smartyard.ui.player.TimeLineAdapter
import ru.madbrains.smartyard.ui.player.TimeLineArchiveAdapter
import java.math.BigDecimal
import java.math.RoundingMode


class CustomHorizontalLayoutManager(context: Context) :
    LinearLayoutManager(context, VERTICAL, false) {
    var isScale: Boolean = false

    override fun canScrollVertically(): Boolean {
        return !isScale
    }

    override fun getChildAt(index: Int): View? {
        return super.getChildAt(index)
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        return super.computeScrollVectorForPosition(targetPosition)
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return super.computeVerticalScrollRange(state)
    }

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        super.scrollToPositionWithOffset(position, offset)
    }
}


class ScaleGestureListener(
    private val mExoPlayerViewModel: ExoPlayerViewModel,
    private val rv: RecyclerView,
    context: Context
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var preScaleTime = -1L
    private var customLayoutManager = CustomHorizontalLayoutManager(context)

    init {
        rv.layoutManager = customLayoutManager
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        rv.isNestedScrollingEnabled = false
        with(mExoPlayerViewModel) {
            val scaleFactor = detector.scaleFactor
            val scaleHeight = scaleHeight.value
                ?: ExoPlayerFragment.HEIGHT_TIME_SECTOR
            val newHeight = (scaleFactor * scaleHeight).toInt()

            when (newHeight) {
                in ExoPlayerFragment.HEIGHT_TIME_SECTOR..ExoPlayerFragment.SCALE_FACTOR -> {
                    setScaleHeight(newHeight) { scrollToTime() }
                    rv.adapter?.notifyDataSetChanged()
                }
            }
            if (scaleHeight == ExoPlayerFragment.SCALE_FACTOR) {
                when (scale.value) {
                    ExoPlayerFragment.SCALE_X1 -> {
                        timeLineItemX2.value.let {
                            if (it != null) {
                                mExoPlayerViewModel.setScale(ExoPlayerFragment.SCALE_X2)
                            }
                        }
                        setScaleHeight(ExoPlayerFragment.HEIGHT_TIME_SECTOR)
                        timeInTimer.value.let {
                            if (it != null) {
                                getPlog(it)
                            }
                        }
                    }

                    ExoPlayerFragment.SCALE_X2 -> {
                        Unit
                    }
                }
            }
            if (newHeight < ExoPlayerFragment.HEIGHT_TIME_SECTOR - 8) {
                when (scale.value) {
                    ExoPlayerFragment.SCALE_X1 -> {
                        Unit
                    }

                    ExoPlayerFragment.SCALE_X2 -> {
                        timeLineItemX1.value.let {
                            if (it != null) {
                                mExoPlayerViewModel.setScale(ExoPlayerFragment.SCALE_X1)
                            }
                        }
                        setScaleHeight(ExoPlayerFragment.HEIGHT_TIME_SECTOR * 3 - 5)
                        timeInTimer.value.let {
                            if (it != null) {
                                getPlog(it)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        preScaleTime = mExoPlayerViewModel.timeInTimer.value ?: -1L
        customLayoutManager.isScale = true
        return super.onScaleBegin(detector)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        super.onScaleEnd(detector)
        customLayoutManager.isScale = false
    }

    private fun scrollToTime() {
        val rangeTime = mExoPlayerViewModel.getTimeLineItem().value?.rangeTime
        val timeInTimer = preScaleTime
        val msSeconds = 60000L
        val timeInTimerWithReminder = when (mExoPlayerViewModel.scale.value) {
            ExoPlayerFragment.SCALE_X1 -> {
                val remainder = timeInTimer % (ExoPlayerFragment.SCALE_X1 * msSeconds)
                if (remainder == 0L) {
                    timeInTimer
                } else {
                    timeInTimer + (ExoPlayerFragment.SCALE_X1 * msSeconds) - remainder
                }
            }

            ExoPlayerFragment.SCALE_X2 -> {
                val remainder = timeInTimer % (ExoPlayerFragment.SCALE_X2 * msSeconds)
                if (remainder == 0L) {
                    timeInTimer
                } else {
                    timeInTimer + (ExoPlayerFragment.SCALE_X2 * msSeconds) - remainder
                }
            }

            else -> {
                timeInTimer
            }
        }
        var position = 0
        for (i in rangeTime?.indices!!) {
            if (rangeTime[i].entries.first().key == timeInTimerWithReminder) {
                position = if (i == 0) 0 else i
                break
            }
        }
        val timeReminder = (timeInTimer - timeInTimerWithReminder) / 1000
        val heightInOneSecond =
            BigDecimal(mExoPlayerViewModel.scaleHeight.value!!.toDouble() / (mExoPlayerViewModel.scale.value!! * 60))
        val scrollToY = (BigDecimal(timeReminder) * heightInOneSecond)
        val halfScrollY = scrollToY.setScale(0, RoundingMode.HALF_EVEN)

        customLayoutManager.computeScrollVectorForPosition(halfScrollY.toInt())
        customLayoutManager.scrollToPositionWithOffset(position, halfScrollY.toInt())
    }
}