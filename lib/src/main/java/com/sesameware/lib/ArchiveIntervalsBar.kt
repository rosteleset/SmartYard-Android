package com.sesameware.lib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ArchiveIntervalsBar(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var availableIntervals = mutableListOf<TimeInterval>()
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mBarHeight: Int
    private var timeInterval: TimeInterval? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        timeInterval?.let { timeInterval ->
            if (availableIntervals.size > 0 && timeInterval.durationInMs > 0) {
                val m = width.toFloat() / timeInterval.durationInMs.toFloat()
                for (i in 0 until availableIntervals.size - 1) {
                    canvas.drawRect(
                        (availableIntervals[i].to.timeInMs() - timeInterval.from.timeInMs()) * m, height.toFloat() - mBarHeight.toFloat(),
                        (availableIntervals[i + 1].from.timeInMs() - timeInterval.from.timeInMs()) * m, height.toFloat(),
                        mPaint
                    )
                }

                if (timeInterval.from < availableIntervals[0].from) {
                    canvas.drawRect(
                        0.0f, height.toFloat() - mBarHeight.toFloat(),
                        (availableIntervals[0].from.timeInMs() - timeInterval.from.timeInMs()) * m, height.toFloat(),
                        mPaint
                    )
                }

                if (timeInterval.to > availableIntervals.last().to) {
                    canvas.drawRect(
                        (availableIntervals.last().to.timeInMs() - timeInterval.from.timeInMs()) * m, height.toFloat() - mBarHeight.toFloat(),
                        width.toFloat(), height.toFloat(),
                        mPaint
                    )
                }
            } else {
                canvas.drawRect(0.0f, height.toFloat() - mBarHeight.toFloat(), width.toFloat(), height.toFloat(), mPaint)
            }
        }
    }

    fun setAvailableIntervals(timeInterval: TimeInterval?, intervals: List<TimeInterval>) {
        this.timeInterval = timeInterval
        availableIntervals = intervals.toMutableList()
        invalidate()
    }

    fun setBarHeight(barHeight: Int) {
        mBarHeight = barHeight
        invalidate()
    }

    init {
        mPaint.color = Color.RED
        mBarHeight = context.resources.getDimensionPixelSize(R.dimen.archive_bar_height)
    }
}
