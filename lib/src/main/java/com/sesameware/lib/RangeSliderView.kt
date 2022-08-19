package com.sesameware.lib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ViewGroup
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Create On 16/10/2016
 * @author wayne
 */

@SuppressLint("ViewConstructor")
class RangeSliderView(
    context: Context,
    private val overlayView: OverlayView,
    private val op: RangeSliderSettings
) : ViewGroup(context) {
    private var mCurrentDate: LocalDate? = null
    private val mBubbleFormatterShort = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val mBubbleFormatterLong = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss")
    private var mTrimMode: Boolean = true
    private var mLeftThumb: ThumbView
    private var mRightThumb: ThumbView
    private var mSeekThumb: ThumbView
    var interval: TimeInterval? = null

    private var mOriginalX = 0
    private var mLastX = 0

    private var mIsDragging = false
    private var mSeekListener: SeekListener? = null
    private var mTrimMoveListener: SeekListener? = null
    private var mSelectionListener: SelectionListener? = null
    private val thumbWidth: Int get() {
        val w = if (mTrimMode) {
            op.trimThumbDrawable.intrinsicWidth
        } else {
            op.seekThumbDrawable.intrinsicWidth
        }
        return w
    }

    init {
        mLeftThumb = createTrimThumb(overlayView.bubble1, op.trimThumbDrawable, true, 0)
        mRightThumb = createTrimThumb(overlayView.bubble2, op.trimThumbDrawable, true, op.tickCount)
        mSeekThumb = createTrimThumb(overlayView.bubbleSeek, op.seekThumbDrawable, false, 0)

        addView(mLeftThumb)
        addView(mRightThumb)
        addView(mSeekThumb)
        setTrimMode(op.trimMode)
        setWillNotDraw(false)
    }

    private fun createTrimThumb(bubbleView: BubbleView, drawable: Drawable, selectable: Boolean, initialRangeIndex: Int): ThumbView {
        return ThumbView(
            context = context,
            bubbleView = bubbleView,
            mThumbDrawable = drawable,
            count = op.tickCount,
            selectable = selectable,
            initialRangeIndex = initialRangeIndex
        )
    }

    override fun onMeasure(width: Int, height: Int) {
        val widthMeasureSpec =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(width), MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, height)
        mLeftThumb.measure(widthMeasureSpec, height)
        mRightThumb.measure(widthMeasureSpec, height)
        mSeekThumb.measure(widthMeasureSpec, height)
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        mLeftThumb.layout(0, 0, mLeftThumb.measuredWidth, mLeftThumb.measuredHeight)
        mRightThumb.layout(0, 0, mRightThumb.measuredWidth, mRightThumb.measuredHeight)
        mSeekThumb.layout(0, 0, mSeekThumb.measuredWidth, mSeekThumb.measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        refreshPositions()
    }
    private fun refreshPositions() {
        if (mTrimMode) {
            mRightThumb.refreshPosition()
            mLeftThumb.refreshPosition()
        } else {
            mSeekThumb.refreshPosition()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mTrimMode) {
            drawTransparentBG(canvas)
        }
    }

    private fun drawTransparentBG(canvas: Canvas) {
        val width = measuredWidth
        val height = measuredHeight
        val lThumbOffset = mLeftThumb.x
        val rThumbOffset = mRightThumb.x

        if (lThumbOffset > 0f) {
            canvas.drawRect(
                0f,
                0f,
                lThumbOffset + thumbWidth,
                height.toFloat(),
                op.bgPaint
            )
        }
        if (rThumbOffset < width - thumbWidth) {
            canvas.drawRect(rThumbOffset, 0f, width.toFloat(), height.toFloat(), op.bgPaint)
        }
    }

    private fun ThumbView.press(x: Int, y: Int): Boolean {
        return if (!isPressed && inInTarget(x, y)) {
            isPressed = true
            true
        } else false
    }

    private fun getActiveThumb(): ThumbView? {
        return when {
            mLeftThumb.isPressed -> mLeftThumb
            mRightThumb.isPressed -> mRightThumb
            mSeekThumb.isPressed -> mSeekThumb
            else -> null
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        var handle = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt()
                val y = event.y.toInt()
                mOriginalX = x
                mLastX = mOriginalX
                mIsDragging = false
                if (mTrimMode) {
                    val pressed = if (abs(x - mLeftThumb.x) < abs(x - mRightThumb.x)) mLeftThumb.press(x, y) else mRightThumb.press(x, y)
                    if (pressed) {
                        handle = true
                    }
                } else if (mSeekThumb.press(x, y)) {
                    handle = true
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mIsDragging = false
                mLastX = 0
                mOriginalX = mLastX
                parent.requestDisallowInterceptTouchEvent(false)
                getActiveThumb()?.run {
                    this.isPressed = false
                    invalidate()
                    handle = true
                    if (selectable) {
                        notifySelectionChange()
                    } else {
                        notifySeekChange()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x.toInt()
                if (!mIsDragging && abs(x - mOriginalX) > op.touchSlop) {
                    mIsDragging = true
                }
                if (mIsDragging) {
                    val moveX = x - mLastX
                    getActiveThumb()?.let {
                        parent.requestDisallowInterceptTouchEvent(true)
                        val toX = moveX + it.x
                        it.moveAndLimit(toX, true)
                        it.updateTimeFromPosition()
                        handle = true
                        invalidate()
                        if (it != mSeekThumb) {
                            mTrimMoveListener?.invoke(it.getProgress())
                        }
                    }
                }
                mLastX = x
            }
        }
        return handle
    }

    private val rangeLength: Float
        get() {
            val width = measuredWidth
            val num = if (width < thumbWidth) {
                1F
            } else (width - thumbWidth).toFloat()
            return num.coerceAtLeast(1F)
        }

    private val intervalLength: Float
        get() = rangeLength / op.tickCount

    private fun getNearestIndex(x: Float): Int {
        return (x / intervalLength).roundToInt()
    }

    fun setSeekChangeListener(seekListener: SeekListener) {
        mSeekListener = seekListener
    }

    fun setTrimMoveListener(seekListener: SeekListener) {
        mTrimMoveListener = seekListener
    }

    fun setSelectionChangeListener(selectionListener: SelectionListener) {
        mSelectionListener = selectionListener
    }

    private fun ThumbView.resetPosition() {
        rangeIndex = initialRangeIndex
        refreshPosition()
    }

    private fun ThumbView.refreshPosition() {
        move(rangeIndex * intervalLength)
    }

    private fun ThumbView.move(offset: Float, checkBubble: Boolean = true, updateIndex: Boolean = false) {
        x = offset
        bubbleView.moveX(offset + mWidth / 2)
        if (checkBubble) {
            overlayView.checkAnimateRightBubble()
        }
        if (updateIndex) {
            rangeIndex = getNearestIndex(offset)
        }
    }

    private fun ThumbView.moveAndLimit(offset: Float, checkBubble: Boolean) {
        val x = when (this) {
            mLeftThumb -> offset.coerceIn(0F, mRightThumb.x - thumbWidth)
            mRightThumb -> offset.coerceIn(mLeftThumb.x + thumbWidth, rangeLength)
            else -> offset.coerceIn(0f, rangeLength)
        }
        this.move(x, checkBubble, updateIndex = true)
    }

    private fun ThumbView.updateTimeFromPosition() {
        interval?.let {
            this.setTime(it.getTimeAtProgress(getProgress()))
        }
    }

    private fun ThumbView.findPosInNewInterval(newInterval: TimeInterval) {
        val progress = newInterval.getProgressWith(currentTime)
        val x = (rangeLength * progress).toFloat()
        moveAndLimit(x, false)
    }

    private fun ThumbView.setTime(it: LocalDateTime) {
        this.currentTime = it
        val formatter = if (it.toLocalDate() == mCurrentDate) mBubbleFormatterShort else mBubbleFormatterLong
        bubbleView.setTimeText(it.format(formatter))
    }

    private fun ThumbView.moveToMs(ms: Long) {
        interval?.let { interval ->
            if (!mIsDragging) {
                val percent = ms.toDouble() / interval.durationInMs
                moveToPercent(percent)
                val newTime = interval.from.plusSeconds(ms / 1000)
                setTime(newTime)
            }
        }
    }
    private fun ThumbView.shiftByMs(ms: Long) {
        interval?.let { interval ->
            val current = getProgress()
            val by = ms.toDouble() / interval.durationInMs
            moveToPercent((current + by).coerceIn(0.0, 1.0))
            updateTimeFromPosition()
        }
    }

    private fun ThumbView.moveToPercent(percent: Double) {
        val x = (rangeLength * percent).toFloat()
        moveAndLimit(x, true)
    }

    fun setIntervalPlayer(newInterval: TimeInterval) {
        interval = newInterval
        mRightThumb.updateTimeFromPosition()
        mLeftThumb.updateTimeFromPosition()
        mSeekThumb.updateTimeFromPosition()
    }

    fun setIntervalTrimmer(newInterval: TimeInterval, reset: Boolean) {
        interval = newInterval
        if (reset) {
            mLeftThumb.resetPosition()
            mRightThumb.resetPosition()
        } else {
            mRightThumb.findPosInNewInterval(newInterval)
            mLeftThumb.findPosInNewInterval(newInterval)
        }

        mRightThumb.updateTimeFromPosition()
        mLeftThumb.updateTimeFromPosition()
        overlayView.checkAnimateRightBubble()
        notifySelectionChange()
        invalidate()
    }

    fun shiftPickerPositionByMs(ms: Long) {
        mRightThumb.shiftByMs(ms)
        mLeftThumb.shiftByMs(ms)
        mRightThumb.updateTimeFromPosition()
        mLeftThumb.updateTimeFromPosition()

        notifySelectionChange()
    }

    private fun notifySelectionChange() {
        mSelectionListener?.invoke(mLeftThumb.currentTime, mRightThumb.currentTime)
    }
    private fun notifySeekChange() {
        mSeekListener?.invoke(mSeekThumb.getProgress())
    }

    private fun setTrimMode(active: Boolean) {
        mTrimMode = active
        mLeftThumb.show(active)
        mRightThumb.show(active)
        mSeekThumb.show(!active)
        refreshPositions()
    }

    fun setSeekFromPlayer(ms: Long) {
        mSeekThumb.moveToMs(ms)
    }

    fun setCurrentDate(date: LocalDate) {
        mCurrentDate = date
    }

    data class RangeSliderSettings(
        val bgPaint: Paint,
        val touchSlop: Int,
        val tickCount: Int,
        val trimThumbDrawable: Drawable,
        val seekThumbDrawable: Drawable,
        val trimMode: Boolean
    )
}
