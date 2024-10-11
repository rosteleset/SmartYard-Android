package ru.madbrains.smartyard.ui.main.address.cctv_video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.annotation.Nullable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class TimeLine @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    private var mPaint: Paint? = null
    private var VIEW_BACKGROUND_COLOR = 0x00000000
    private val widthTimes = 1.5f
    private var videos: List<PlaybackVo>? = ArrayList()
    private var SECOND_WIDTH = 30f //px
    private var MIN_SECOND_WIDTH = 5f
    private var MAX_SECOND_WIDTH = 30f
    private val midBackgroundWidth = 5 //dp
    private var mWidth = 0f
    private var mHeight = 0f
    private var mDensity = 0f  // Screen density dp*mDensity=px
    private var mScroller: Scroller? = null
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinVelocity = 0
    private var mValue: TimeAlgorithm? = null // The current scale indicates the time
    private var selectValue: TimeAlgorithm? = null //Specify the moving time

    private fun init(context: Context) {
        mScroller = Scroller(context)
        mVelocityTracker = VelocityTracker.obtain()
        mDensity = context.resources.displayMetrics.density
        mPaint = Paint()
        mPaint!!.color = SCALE_COLOR
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mValue = TimeAlgorithm(System.currentTimeMillis() * 1000)
        mMinVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        //   Log.d(TAG, "init: " + mDensity);
    }

    fun moveToValue(micros: Long) {
        if (micros == mValue!!.timeInMicros) return
        val msg: Message = Message.obtain()
        msg.what = SET_VALUE
        selectValue = TimeAlgorithm(micros)
        if (isCurrentDay(mValue, selectValue!!)) {
            moveX =
                (selectValue!!.timeInSecond - mValue!!.timeInSecond) * SECOND_WIDTH / SET_VALUE_AUTO_MOVE_COUNT
            if (Math.abs(moveX) < SECOND_WIDTH && moveX != 0f) {
                moveX = moveX * SECOND_WIDTH / moveX
            }
            //    Log.d(TAG, "moveToValue: " + moveX + ">>" + SECOND_WIDTH);
            handler.sendMessage(msg)
        }
    }

    fun setValue(micros: Long) {
        removeAllMessage()
        mValue = TimeAlgorithm(micros)
        postInvalidate()
        if (listener != null) {
            listener!!.onValueChanged(mValue!!.stringTimeInSecond, mValue!!.timeInMicros)
        }
        handler.sendEmptyMessageDelayed(DELAY_MOVE_TO_NEARBY_VIDEO,
            delayMoveToNearbyVideoMs.toLong()
        )
    }

    private fun removeAllMessage() {
        handler.removeMessages(DELAY_MOVE_TO_NEARBY_VIDEO)
        handler.removeMessages(SET_VALUE)
    }

    fun setViewBackgroundColor(color: Int) {
        VIEW_BACKGROUND_COLOR = color
    }

    fun setVideos(videos: List<PlaybackVo>?) {
        if (videos == null || videos.size == 0) return
        this.videos = videos
        moveToNearbyVideo()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Log.d(TAG, "onSizeChanged: ");
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        MAX_SECOND_WIDTH = mWidth / 1800
        MIN_SECOND_WIDTH = mWidth / (60 * 60 * 24)
        SECOND_WIDTH = MAX_SECOND_WIDTH
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Log.d(TAG, "onLayout:  " + changed + ">>left=" + left + ">>top=" + top + ">>right=" + right + ">>bottom=" + bottom);
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //  Log.d(TAG, "onDraw: ");
    }

    private val SET_VALUE_AUTO_MOVE_TIME = 100 //ms
    private val SET_VALUE_AUTO_MOVE_COUNT = 10 //times
    private var moveX = 0f
    private val delayMoveToNearbyVideoMs = 1000 //ms
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (hasMessages(DELAY_MOVE_TO_NEARBY_VIDEO)) {
                removeMessages(DELAY_MOVE_TO_NEARBY_VIDEO)
            }
            when (msg.what) {
                SET_VALUE -> autoMoving()
                DELAY_MOVE_TO_NEARBY_VIDEO -> moveToNearbyVideo()
            }
        }
    }

    private fun autoMoving() {
        val value = mValue!!.addOrSub((moveX / SECOND_WIDTH).toInt())
        if (moveX > 0) {
            if (value.timeInMicros > selectValue!!.timeInMicros) {
                mValue = TimeAlgorithm(selectValue!!.timeInMicros)
                // Log.d(TAG, "autoMoving: >");
                invalidate()
                handler.removeMessages(SET_VALUE)
            }
        } else if (moveX < 0) {
            if (value.timeInMicros < selectValue!!.timeInMicros) {
                mValue = TimeAlgorithm(selectValue!!.timeInMicros)
                invalidate()
                // Log.d(TAG, "autoMoving: <");
                handler.removeMessages(SET_VALUE)
            }
        }
        if (mValue!!.timeInMicros != selectValue!!.timeInMicros) {
            moveX(moveX)
            handler.sendEmptyMessageDelayed(
                SET_VALUE,
                (SET_VALUE_AUTO_MOVE_TIME / SET_VALUE_AUTO_MOVE_COUNT).toLong()
            )
        } else {
            if (listener != null) {
                listener!!.onVideoStart(mValue!!.stringTimeInSecond, mValue!!.timeInMicros)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // Log.d(TAG, "dispatchDraw: ")；
        // canvas.drawColor(VIEW_BACKGROUND_COLOR);
        drawVideos(canvas)
        drawScale(canvas)
        drawMidLine(canvas)
    }

    private val VIDEO_PADDING = 1
    private fun drawVideos(canvas: Canvas) {
        if (videos == null || videos!!.size == 0) return
        canvas.save()
        for (i in videos!!.indices) {
            val vo = videos!![i]
            val startX =
                mWidth / 2 + (vo.startTA.timeInSecond - mValue!!.timeInSecond) * SECOND_WIDTH
            val endX = mWidth / 2 + (vo.endTA.timeInSecond - mValue!!.timeInSecond) * SECOND_WIDTH
            if (endX < 0 || startX > mWidth) continue
            val rectF = RectF(startX, VIDEO_PADDING.toFloat(), endX, mHeight - VIDEO_PADDING)
            mPaint!!.color = VIDEO_COLOR
            mPaint!!.style = Paint.Style.FILL
            canvas.drawRect(rectF, mPaint!!)
        }
        canvas.restore()
    }

    private fun drawScale(canvas: Canvas) {
        canvas.save()
        mPaint!!.color = SCALE_COLOR
        mPaint!!.strokeWidth = mDensity
        val scaleMode = scaleMode
        val mod = mValue!!.mod(scaleMode)
        var drawWidth = 0f
        var i = 0
        while (drawWidth < mWidth * 1.1) {
            var height = TWO_MIN_SCALE_HEIGHT
            var x = 0f
            var mode = 0
            x = mWidth / 2 + (scaleMode - mod + scaleMode * i) * SECOND_WIDTH
            val rightTime = mValue!!.addOrSub(scaleMode - mod + scaleMode * i)
            var time = rightTime.stringTimeInMinute
            val rect = Rect()
            //            Log.d(TAG, "isCurrentDayContainZero: " + rightTime.getStringTimeInSecond() + ">>" + scaleMode
//                    + ">>" + isCurrentDayContainZero(mValue, rightTime) + ">>" + isDrawScaleLine(rightTime)
//                    + ">>" + mValue.getTimeInMillis() + ">>" + mValue.getCurrentMaxTimeInMillis() + ">>" + mValue.getCurrentMinTimeInMillis() + ">>" + rightTime.getTimeInMillis());
            if (isCurrentDayContainZero(mValue, rightTime)) {
                mode = rightTime.scaleMode
                height = getScaleHeight(rightTime) * mDensity
                if (isDrawScaleLine(rightTime)) {
                    canvas.drawLine(x, 0f, x, height, mPaint!!)
                    canvas.drawLine(x, mHeight, x, mHeight - height, mPaint!!)
                }
                mPaint!!.textSize = SCALE_TEXT_SIZE * mDensity
                mPaint!!.getTextBounds(time, 0, time.length, rect)
                if (isDrawText(rightTime, rect.width() * widthTimes)) {
                    if (time == "00:00") time = "24:00"
                    canvas.drawText(
                        time, x - rect.width() / 2,
                        height + rect.height() + 2 * mDensity,
                        mPaint!!
                    )
                }
            }
            x = mWidth / 2 - (mod + scaleMode * i) * SECOND_WIDTH
            val leftTime = mValue!!.addOrSub(-(mod + scaleMode * i))
            height = getScaleHeight(leftTime) * mDensity
            if (isCurrentDayContainZero(mValue, leftTime)) {
                if (isDrawScaleLine(leftTime)) {
                    canvas.drawLine(x, 0f, x, height, mPaint!!)
                    canvas.drawLine(x, mHeight, x, mHeight - height, mPaint!!)
                }
                time = leftTime.stringTimeInMinute
                mPaint!!.getTextBounds(time, 0, time.length, rect)
                mode = leftTime.scaleMode
                if (isDrawText(leftTime, rect.width() * widthTimes)) {
                    canvas.drawText(
                        time, x - rect.width() / 2,
                        height + rect.height() + 2 * mDensity,
                        mPaint!!
                    )
                }
            }
            drawWidth += 2 * scaleMode * SECOND_WIDTH
            i++
        }
        mPaint!!.strokeWidth = 1.5f * mDensity
        canvas.drawLine(0f, 0f, mWidth, 0f, mPaint!!)
        canvas.drawLine(0f, mHeight, mWidth, mHeight, mPaint!!)
        canvas.restore()
    }

    private fun drawMidLine(canvas: Canvas) {
        canvas.save()
        //  Log.d(TAG, "drawMidLine: width=" + mWidth + ">>height=" + mHeight);
        mPaint!!.color = MID_BACKGROUND_COLOR
        mPaint!!.style = Paint.Style.FILL
        val rectF = RectF(
            (mWidth - midBackgroundWidth * mDensity) / 2,
            0f,
            (mWidth + midBackgroundWidth * mDensity) / 2,
            mHeight
        )
        canvas.drawRect(rectF, mPaint!!)
        mPaint!!.strokeWidth = 1.5f * mDensity
        mPaint!!.color = MID_COLOR
        canvas.drawLine(mWidth / 2, 0f, mWidth / 2, mHeight, mPaint!!)
        canvas.restore()
        // Log.d(TAG, "drawMidLine: " + mValue.getStringTimeInSecond());
    }

    private fun isCurrentDayContainZero(current: TimeAlgorithm?, compare: TimeAlgorithm): Boolean {
        var isCurrentDay = true
        if (current!!.currentMaxTimeInMillis / 1000 + 1 < compare.timeInMillis / 1000) {
            isCurrentDay = false
        } else if (current.currentMinTimeInMillis > compare.timeInMillis) {
            isCurrentDay = false
        }
        return isCurrentDay
    }

    private fun isCurrentDay(current: TimeAlgorithm?, compare: TimeAlgorithm): Boolean {
        var isCurrentDay = true
        if (current!!.currentMaxTimeInMillis / 1000 < compare.timeInMillis / 1000) {
            isCurrentDay = false
        } else if (current.currentMinTimeInMillis > compare.timeInMillis) {
            isCurrentDay = false
        }
        return isCurrentDay
    }

    private val scaleMode: Int
        private get() {
            var mode = TWO_MINUTE_SCALE_INTERVAL
            if (ONE_HOUR_SCALE_INTERVAL * SECOND_WIDTH <= TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH) {
                mode = ONE_HOUR_SCALE_INTERVAL
            } else if (TWENTY_MINUTE_SCALE_INTERVAL * SECOND_WIDTH <= TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH) {
                mode = TWENTY_MINUTE_SCALE_INTERVAL
            } else if (TEN_MINUTE_SCALE_INTERVAL * SECOND_WIDTH <= 2 * TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH) {
                mode = TEN_MINUTE_SCALE_INTERVAL
            }
            return mode
        }

    private fun isDrawScaleLine(time: TimeAlgorithm): Boolean {
        var draw = false
        when (time.scaleMode) {
            TimeAlgorithm.MODE_TWO_MINUTE -> {
                draw = TEN_MINUTE_SCALE_INTERVAL * SECOND_WIDTH > TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH * 2
            }
            TimeAlgorithm.MODE_TEN_MINUTE -> {
                val twentyMinuteCondition = time.isTwentyMinuteMultiple && ONE_HOUR_SCALE_INTERVAL * SECOND_WIDTH > 2 * TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH || !time.isTwentyMinuteMultiple
                val twentyMinuteScale = TWENTY_MINUTE_SCALE_INTERVAL * SECOND_WIDTH
                draw = twentyMinuteScale > 1.5 * TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH && twentyMinuteCondition
            }
            TimeAlgorithm.MODE_HOUR -> {
                draw = ONE_HOUR_SCALE_INTERVAL * SECOND_WIDTH > TWO_MINUTE_SCALE_INTERVAL * MAX_SECOND_WIDTH || time.isTwoHourMultiple
            }
        }
        return draw
    }

    private fun isDrawText(time: TimeAlgorithm, width: Float): Boolean {
        return if (time.scaleMode < TimeAlgorithm.MODE_TEN_MINUTE) false else (TEN_MINUTE_SCALE_INTERVAL * SECOND_WIDTH > width || TWENTY_MINUTE_SCALE_INTERVAL * SECOND_WIDTH > width && time.isTwentyMinuteMultiple || time.scaleMode == TimeAlgorithm.MODE_HOUR && ONE_HOUR_SCALE_INTERVAL * SECOND_WIDTH > width
                || time.isTwoHourMultiple)
        //  Log.d(TAG, "isDrawText: " + time.isTwoHourMultiple() + ">>" + time.getStringTimeInSecond());
    }

    private fun getScaleHeight(t: TimeAlgorithm): Float {
        var height = TWO_MIN_SCALE_HEIGHT
        when (t.scaleMode) {
            TimeAlgorithm.MODE_HOUR -> height = HOUR_SCALE_HEIGHT
            TimeAlgorithm.MODE_TEN_MINUTE -> height = TEN_MIN_SCALE_HEIGHT
            TimeAlgorithm.MODE_TWO_MINUTE -> height = TWO_MIN_SCALE_HEIGHT
        }
        return height
    }

    private var downOneX = 0f
    private var downTwoX = 0f
    private var downOneY = 0f
    private var downTwoY = 0f
    private var clickCount = 0
    private var distance = 0f
    private var needDelay = false
    private var clickTime //ms
            : Long = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mVelocityTracker!!.addMovement(event)
        lastX = 0
        mScroller!!.forceFinished(true)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeMessages(DELAY_MOVE_TO_NEARBY_VIDEO)
                handler.removeMessages(SET_VALUE)
                downOneX = event.getX(0)
                downOneY = event.getY(0)
                clickCount++
                if (clickCount == 2 && System.currentTimeMillis() - clickTime > 100) { //Anti-shake
                    SECOND_WIDTH =
                        MAX_SECOND_WIDTH //SECOND_WIDTH == MAX_SECOND_WIDTH ? MIN_SECOND_WIDTH : MAX_SECOND_WIDTH;
                    // Log.d(TAG, "onTouchEvent: Restore >>" + (System.currentTimeMillis()-clickTime));
                    invalidate()
                }
                clickTime = System.currentTimeMillis()
                handler.postDelayed(Runnable { clickCount = 0 }, 200)
                //  Log.d(TAG, "onTouchEvent: ACTION_DOWN " + downOneX + ">>" + event.getPointerCount());
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount > 1) {
                downTwoX = event.getX(1)
                downTwoY = event.getY(1)
            }

            MotionEvent.ACTION_MOVE -> move(event)
            MotionEvent.ACTION_POINTER_UP -> {
                distance = 0f
                needDelay = true
                handler.postDelayed(Runnable { needDelay = false }, 200)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Log.d(TAG, "onTouchEvent: ACTION_UP");
                distance = 0f
                autoScroll()
                needDelay = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun autoScroll() {
        if (needDelay) {
            handler.sendEmptyMessageDelayed(DELAY_MOVE_TO_NEARBY_VIDEO,
                delayMoveToNearbyVideoMs.toLong()
            )
            return
        }
        mVelocityTracker!!.computeCurrentVelocity(mWidth.toInt(), Float.MAX_VALUE)
        val xVelocity = mVelocityTracker!!.xVelocity
        if (Math.abs(xVelocity) > mMinVelocity) {
            //   Log.d(TAG, "autoScroll: " + xVelocity + ">>" + mMinVelocity);
            mScroller!!.forceFinished(false)
            mScroller!!.fling(0, 0, xVelocity.toInt(), 0, Int.MIN_VALUE, Int.MAX_VALUE, 0, 0)
            postInvalidate()
        } else if (listener != null) {
            //  Log.d(TAG, "autoScroll: DELAY_MOVE_TO_NEARBY_VIDEO");
            handler.sendEmptyMessageDelayed(DELAY_MOVE_TO_NEARBY_VIDEO,
                delayMoveToNearbyVideoMs.toLong()
            )
        }
    }

    @Synchronized
    private fun move(event: MotionEvent) {
        val num = event.pointerCount
        if (num > 1) {
            val d = Math.abs(event.getX(1) - event.getX(0))
            distance = Math.abs(downTwoX - downOneX)
            val scale = 1 + (d - distance) / distance
            var width = SECOND_WIDTH
            width *= scale
            if (width > MAX_SECOND_WIDTH) {
                width = MAX_SECOND_WIDTH
            } else if (width < MIN_SECOND_WIDTH) {
                width = MIN_SECOND_WIDTH
            }
            if (width != SECOND_WIDTH) {
                SECOND_WIDTH = width
                invalidate()
            }
            //            Log.d(TAG, "onTouchEvent: ACTION_MOVE 22  scale= " + scale);
            downOneX = event.getX(0)
            downOneY = event.getY(0)
            downTwoX = event.getX(1)
            downTwoY = event.getY(1)
            distance = d
        } else {
            if (!needDelay) {
                val move = downOneX - event.getX(0)
                // Log.d(TAG, "onTouchEvent: ACTION_MOVE " + move + ">>" + SECOND_WIDTH);
                moveX(move)
            }
            downOneX = event.getX(0)
            downOneY = event.getY(0)
        }
    }

    private fun moveX(move: Float) {
        val sec = (move / SECOND_WIDTH).toInt()
        val value = mValue!!.addOrSub(sec)
        if (value.timeInMillis < mValue!!.currentMinTimeInMillis) {
            if (mScroller != null) mScroller!!.forceFinished(true)
            value.timeInMillis = mValue!!.currentMinTimeInMillis
        } else if (value.timeInMillis > mValue!!.currentMaxTimeInMillis) {
            if (mScroller != null) mScroller!!.forceFinished(true)
            value.timeInMillis = mValue!!.currentMaxTimeInMillis
        }
        if (value.timeInMillis != mValue!!.timeInMillis) {
            mValue = value
            if (listener != null) {
                listener!!.onValueChanged(mValue!!.stringTimeInSecond, mValue!!.timeInMicros)
            }
            postInvalidate()
        }
    }

    private var lastX = 0
    override fun computeScroll() {
        super.computeScroll()
        if (mScroller!!.computeScrollOffset()) {
            if (mScroller!!.currX == mScroller!!.finalX) {
                lastX = 0
                handler.sendEmptyMessageDelayed(
                    DELAY_MOVE_TO_NEARBY_VIDEO,
                    delayMoveToNearbyVideoMs.toLong()
                )
                //   Log.d(TAG, "computeScroll: end DELAY_MOVE_TO_NEARBY_VIDEO");
            } else {
                val xPosition = mScroller!!.currX
                //   Log.d(TAG, "computeScroll: scroll " + lastX + ">>" + xPosition + ">>" + SECOND_WIDTH);
                if ((lastX - xPosition) / SECOND_WIDTH == 0f) {
                    handler.sendEmptyMessageDelayed(
                        DELAY_MOVE_TO_NEARBY_VIDEO,
                        delayMoveToNearbyVideoMs.toLong()
                    )
                } else {
                    moveX((lastX - xPosition).toFloat())
                }
                lastX = xPosition
            }
        } else {
            //  Log.d(TAG, "computeScrollOffset: false");
        }
    }

    private fun moveToNearbyVideo() {
        if (videos == null || videos!!.size == 0) {
            if (listener != null) listener!!.onNoneVideo(
                mValue!!.stringTimeInSecond,
                mValue!!.timeInMicros
            )
            return
        }
        var value: Long = 0
        for (i in videos!!.indices) {
            val vo = videos!![i]
            if (vo.startTime < mValue!!.timeInMicros && vo.endTime > mValue!!.timeInMicros) {
                // Log.d(TAG, "moveToNearbyVideo: between video");
                if (listener != null) listener!!.onVideoStart(
                    mValue!!.stringTimeInSecond,
                    mValue!!.timeInMicros
                )
                return
            }
            if (vo.startTime > mValue!!.timeInMicros && isCurrentDay(mValue, vo.startTA)) {
                value = vo.startTime
                break
            }
        }
        //   Log.d(TAG, "moveToNearbyVideo: " + value);
        if (value != 0L) {
            moveToValue(value)
        } else {
            if (listener != null) listener!!.onNoneVideo(
                mValue!!.stringTimeInSecond,
                mValue!!.timeInMicros
            )
        }
    }

    private fun getMaxAbs(moveOneX: Float, moveTwoX: Float): Float {
        return if (Math.abs(moveOneX) > Math.abs(moveTwoX)) Math.abs(moveOneX) else Math.abs(
            moveTwoX
        )
    }

    fun setOnPlaybackViewListener(listener: PlaybackViewListener) {
        this.listener = listener
        listener.onValueChanged(mValue!!.stringTimeInSecond, mValue!!.timeInMicros)
    }

    private var listener: PlaybackViewListener? = null

    init {
        init(context)
    }

    interface PlaybackViewListener {
        fun onValueChanged(timeInMillis: String?, timeInMicros: Long)
        fun onVideoStart(timeInMillis: String?, timeInMicros: Long)
        fun onNoneVideo(timeInMillis: String?, timeInMicros: Long)
    }

    class TimeAlgorithm( //microsecond
        var timeInMicros: Long
    ) {

        val stringTimeInSecond: String
            get() {
                val sdf = SimpleDateFormat("HH:mm:ss")
                val date = Date(timeInMicros / 1000)
                return sdf.format(date)
            }
        val stringTimeInMinute: String
            get() {
                val sdf = SimpleDateFormat("HH:mm")
                val date = Date(timeInMicros / 1000)
                return sdf.format(date)
            }
        val isTwentyMinuteMultiple: Boolean
            get() {
                val date = Date(timeInMicros / 1000)
                val calendarObj: Calendar = Calendar.getInstance()
                calendarObj.setTime(date)
                val m: Int = calendarObj.get(Calendar.MINUTE)
                val s: Int = calendarObj.get(Calendar.SECOND)
                return (m * 60 + s) % 1200 == 0
            }

        // Log.d(TAG, "getCurrentMaxTimeInMillis: " + (calendarObj.getTimeInMillis() - 1000));
        val currentMaxTimeInMillis: Long
            get() {
                val date = Date(timeInMicros / 1000)
                val calendarObj: Calendar = Calendar.getInstance()
                calendarObj.setTime(date)
                calendarObj.set(Calendar.HOUR_OF_DAY, 24)
                calendarObj.set(Calendar.MINUTE, 0)
                calendarObj.set(Calendar.SECOND, 0)
                calendarObj.set(Calendar.MILLISECOND, 0)
                // Log.d(TAG, "getCurrentMaxTimeInMillis: " + (calendarObj.getTimeInMillis() - 1000));
                return calendarObj.getTimeInMillis() - 1000
            }

        // Log.d(TAG, "getCurrentMinTimeInMillis: " + calendarObj.getTimeInMillis());
        val currentMinTimeInMillis: Long
            get() {
                val date = Date(timeInMicros / 1000)
                val calendarObj: Calendar = Calendar.getInstance()
                calendarObj.setTime(date)
                calendarObj.set(Calendar.HOUR_OF_DAY, 0)
                calendarObj.set(Calendar.MINUTE, 0)
                calendarObj.set(Calendar.SECOND, 0)
                calendarObj.set(Calendar.MILLISECOND, 0)
                // Log.d(TAG, "getCurrentMinTimeInMillis: " + calendarObj.getTimeInMillis());
                return calendarObj.getTimeInMillis()
            }

        val isTwoHourMultiple: Boolean
            get() {
                val date = Date(timeInMicros / 1000)
                val calendarObj: Calendar = Calendar.getInstance()
                calendarObj.setTime(date)
                val h: Int = calendarObj.get(Calendar.HOUR)
                val m: Int = calendarObj.get(Calendar.MINUTE)
                val s: Int = calendarObj.get(Calendar.SECOND)
                return (h * 60 * 60 + m * 60 + s) % (2 * 60 * 60) == 0
            }
        val scaleMode: Int
            get() {
                val date = Date(timeInMicros / 1000)
                val calendarObj: Calendar = Calendar.getInstance()
                calendarObj.setTime(date)
                val h: Int = calendarObj.get(Calendar.HOUR_OF_DAY)
                val m: Int = calendarObj.get(Calendar.MINUTE)
                val s: Int = calendarObj.get(Calendar.SECOND)
                var mode = -1
                if ((h * 60 * 60 + 60 * m + s) % 3600 == 0) {
                    mode = MODE_HOUR
                } else if ((60 * m + s) % 600 == 0) {
                    mode = MODE_TEN_MINUTE
                } else if ((60 * m + s) % 120 == 0) {
                    mode = MODE_TWO_MINUTE
                }
                return mode
            }

        // add or subtract _sec seconds
        fun addOrSub(_sec: Int): TimeAlgorithm {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date(timeInMicros / 1000)
            val calendar: Calendar = Calendar.getInstance()
            calendar.setTime(date)
            calendar.add(Calendar.SECOND, _sec)
            val sdf2 = SimpleDateFormat("HH:mm:ss")
            return TimeAlgorithm(calendar.getTimeInMillis() * 1000)
        }

        val timeInSecond: Long
            get() = timeInMicros / (1000 * 1000)

        var timeInMillis: Long
            get() = timeInMicros / 1000
            set(millis) {
                timeInMicros = millis * 1000
            }

        fun mod(_timeInterval: Int): Int {
            val date = Date(timeInMicros / 1000)
            val calendarObj: Calendar = Calendar.getInstance(Locale.CHINA)
            calendarObj.setTime(date)
            val m: Int = calendarObj.get(Calendar.MINUTE)
            val s: Int = calendarObj.get(Calendar.SECOND)
            return (60 * m + s) % _timeInterval
        }

        companion object {
            const val MODE_HOUR = 2
            const val MODE_TEN_MINUTE = 1
            const val MODE_TWO_MINUTE = 0
        }
    }

    class PlaybackVo(val startTime: Long, val endTime: Long, var size: Long, var type: Byte) :
        Serializable {
        val startTA: TimeAlgorithm
        val endTA: TimeAlgorithm

        init {
            startTA = TimeAlgorithm(startTime)
            endTA = TimeAlgorithm(endTime)
        }
    }

    companion object {
        private const val TAG = "PlaybackView>>"
        private const val SCALE_COLOR = -0x838384
        private const val VIDEO_COLOR = -0x5476
        private const val MID_COLOR = -0xfc641b
        private const val MID_BACKGROUND_COLOR = 0x557C7C7C
        private const val TWO_MIN_SCALE_HEIGHT = 5f
        private const val TEN_MIN_SCALE_HEIGHT = 10f
        private const val HOUR_SCALE_HEIGHT = 15f
        private const val TWO_MINUTE_SCALE_INTERVAL = 120
        private const val TEN_MINUTE_SCALE_INTERVAL = 600
        private const val TWENTY_MINUTE_SCALE_INTERVAL = 1200
        private const val ONE_HOUR_SCALE_INTERVAL = 3600
        private const val TWO_HOUR_SCALE_INTERVAL = 7200
        private const val SCALE_TEXT_SIZE = 12
        private const val SET_VALUE = 0
        private const val DELAY_MOVE_TO_NEARBY_VIDEO = 1
    }
}