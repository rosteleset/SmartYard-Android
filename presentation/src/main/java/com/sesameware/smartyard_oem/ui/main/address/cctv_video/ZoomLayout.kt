package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.widget.FrameLayout

class MyGestureDetector(
    private var singleTapListener: () -> Unit = {},
    private var doubleTapListener: (x: Float?) -> Unit = {},
    private var longPressListener: () -> Unit = {}
) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        singleTapListener.invoke()
        return super.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        doubleTapListener.invoke(e?.x)
        return super.onDoubleTap(e)
    }

    override fun onLongPress(e: MotionEvent?) {
        longPressListener.invoke()
        return super.onLongPress(e)
    }
}

/**
 * Layout для зумирования. Должен содержать только один дочерний элемент.
 */
class ZoomLayout : FrameLayout, OnScaleGestureListener {
    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f

    // Место касания экрана пальцем
    private var startX = 0f
    private var startY = 0f

    // Насколько двигать дочерний элемент
    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f

    private var singleTapListener: (() -> Unit)? = null
    private var doubleTapListener: ((x: Float?) -> Unit)? = null

    private var aspectRatio: Float? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init(context)
    }

    fun setSingleTapConfirmeListener(listener: (() -> Unit)?) {
        this.singleTapListener = listener
    }

    fun setDoubleTapConfirmedListener(listener: ((x: Float?) -> Unit)?) {
        this.doubleTapListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init(context: Context) {
        val scaleDetector = ScaleGestureDetector(context, this)
        val q = GestureDetector(context, MyGestureDetector(
            {
                singleTapListener?.invoke()
            }, {
                doubleTapListener?.invoke(it)
            }))
        setOnTouchListener { _, motionEvent ->
            q.onTouchEvent(motionEvent)
            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    //Log.i(TAG, "DOWN")
                    if (scale > MIN_ZOOM) {
                        mode = Mode.DRAG
                        startX = motionEvent.x - prevDx
                        startY = motionEvent.y - prevDy
                    }
                }
                MotionEvent.ACTION_MOVE -> if (mode == Mode.DRAG) {
                    dx = motionEvent.x - startX
                    dy = motionEvent.y - startY
                }
                MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
                MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
                MotionEvent.ACTION_UP -> {
                    //Log.i(TAG, "UP")
                    mode = Mode.NONE
                    prevDx = dx
                    prevDy = dy
                }
            }
            scaleDetector.onTouchEvent(motionEvent)
            if (mode == Mode.DRAG && scale >= MIN_ZOOM || mode == Mode.ZOOM) {
                parent.requestDisallowInterceptTouchEvent(true)

                var childWidth = child().width
                var childHeight = child().height

                //расчет размеров
                aspectRatio?.let {ratio ->
                    if (width > 0 && height > 0) {
                        val lp = child().layoutParams as LayoutParams
                        val layoutRatio = width.toFloat() / height.toFloat()
                        if (layoutRatio > ratio) {
                            childWidth = (height.toFloat() * ratio).toInt()
                            childHeight = height
                        } else {
                            childWidth = width
                            childHeight = (width.toFloat() / ratio).toInt()
                        }

                        lp.width = childWidth
                        lp.height = childHeight
                        lp.gravity = Gravity.NO_GRAVITY
                        child().layoutParams = lp
                        child().requestLayout()
                    }
                }

                val maxDx = childWidth * scale - width
                val maxDy = childHeight * scale - height
                dx = Math.min(Math.max(dx, -maxDx), 0f)
                dy = Math.min(Math.max(dy, -maxDy), 0f)

                //центрируем дочерний элемент, если его размеры меньше родительского
                if (childWidth * scale < width) {
                    dx = (width - childWidth * scale) / 2
                }
                if (childHeight * scale < height) {
                    dy = (height - childHeight * scale) / 2
                }
                
                //Log.i(TAG,"Width: " + child().width + ", scale " + scale + ", dx " + dx + ", max " + maxDx)
                applyScaleAndTranslation()
            }
            true
        }
    }

    // ScaleGestureDetector
    override fun onScaleBegin(scaleDetector: ScaleGestureDetector): Boolean {
        //Log.i(TAG, "onScaleBegin")
        return true
    }

    override fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
        val scaleFactor = scaleDetector.scaleFactor
        //Log.i(TAG, "onScale(), scaleFactor = $scaleFactor")
        if (lastScaleFactor == 0f || Math.signum(scaleFactor) == Math.signum(lastScaleFactor)) {
            val prevScale = scale
            scale *= scaleFactor
            scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM))
            lastScaleFactor = scaleFactor
            val adjustedScaleFactor = scale / prevScale
            //Log.d(TAG, "onScale, adjustedScaleFactor = $adjustedScaleFactor")
            //Log.d(TAG, "onScale, BEFORE dx/dy = $dx/$dy")
            val focusX = scaleDetector.focusX
            val focusY = scaleDetector.focusY
            //Log.d(TAG, "onScale, focusX/focusy = $focusX/$focusY")
            dx += (dx - focusX) * (adjustedScaleFactor - 1)
            dy += (dy - focusY) * (adjustedScaleFactor - 1)
            //Log.d(TAG, "onScale, dx/dy = $dx/$dy")
        } else {
            lastScaleFactor = 0f
        }
        return true
    }

    override fun onScaleEnd(scaleDetector: ScaleGestureDetector) {
        //Log.i(TAG, "onScaleEnd")
    }

    private fun applyScaleAndTranslation() {
        child().scaleX = scale
        child().scaleY = scale
        child().pivotX = 0.0f // по умолчанию опорная точка в левом верхнем углу
        child().pivotY = 0.0f
        child().translationX = dx
        child().translationY = dy
    }

    private fun child(): View {
        return getChildAt(0)
    }

    fun resetZoom() {
        mode = Mode.NONE
        scale = 1.0f
        lastScaleFactor = 0f

        startX = 0f
        startY = 0f

        dx = 0f
        dy = 0f
        prevDx = 0f
        prevDy = 0f

        val lp = child().layoutParams as LayoutParams
        lp.width = LayoutParams.WRAP_CONTENT
        lp.height = LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.CENTER
        child().layoutParams = lp
        child().requestLayout()

        applyScaleAndTranslation()
    }

    fun setAspectRatio(ratio: Float?) {
        aspectRatio = ratio
    }

    companion object {
        private const val TAG = "ZoomLayout"
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 8.0f
    }
}
