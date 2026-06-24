package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

class TouchInterceptorZoomLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), ScaleGestureDetector.OnScaleGestureListener {

    private enum class Mode { NONE, DRAG, ZOOM }
    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f

    private var startX = 0f
    private var startY = 0f
    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f

    private var aspectRatio: Float? = null

    private var singleTapListener: (() -> Unit)? = null
    private var doubleTapListener: ((Float?) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, this)
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            singleTapListener?.invoke()
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleTapListener?.invoke(e.x)
            return true
        }
    })

    fun setSingleTapConfirmedListener(listener: (() -> Unit)?) { singleTapListener = listener }
    fun setDoubleTapConfirmedListener(listener: ((Float?) -> Unit)?) { doubleTapListener = listener }

    fun setAspectRatio(ratio: Float?) {
        aspectRatio = ratio
        post { updateChildBounds() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            updateChildBounds()
            resetZoom()
        }
    }

    private fun updateChildBounds() {
        if (childCount == 0) return
        val child = getChildAt(0)
        var childWidth = width
        var childHeight = height

        aspectRatio?.let { ratio ->
            if (width > 0 && height > 0) {
                val layoutRatio = width.toFloat() / height.toFloat()
                if (layoutRatio > ratio) {
                    childWidth = (height.toFloat() * ratio).toInt()
                    childHeight = height
                } else {
                    childWidth = width
                    childHeight = (width.toFloat() / ratio).toInt()
                }
            }
        }

        val lp = child.layoutParams as LayoutParams
        if (lp.width != childWidth || lp.height != childHeight) {
            lp.width = childWidth
            lp.height = childHeight
            lp.gravity = Gravity.CENTER
            child.layoutParams = lp
            child.requestLayout()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(motionEvent)
        scaleDetector.onTouchEvent(motionEvent)

        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (scale > 1.0f) {
                    mode = Mode.DRAG
                    startX = motionEvent.x - prevDx
                    startY = motionEvent.y - prevDy
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG) {
                    dx = motionEvent.x - startX
                    dy = motionEvent.y - startY
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
            MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = Mode.NONE
                prevDx = dx
                prevDy = dy
            }
        }

        if (mode == Mode.DRAG || mode == Mode.ZOOM) {
            parent.requestDisallowInterceptTouchEvent(true)
            applyBounds()
            applyScaleAndTranslation()
        }
        return true
    }

    override fun onScaleBegin(scaleDetector: ScaleGestureDetector): Boolean = true

    override fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
        val scaleFactor = scaleDetector.scaleFactor
        if (lastScaleFactor == 0f || Math.signum(scaleFactor) == Math.signum(lastScaleFactor)) {
            val prevScale = scale
            scale *= scaleFactor
            scale = max(1.0f, min(scale, 8.0f))
            lastScaleFactor = scaleFactor

            val adjustedScaleFactor = scale / prevScale
            dx += (dx - (scaleDetector.focusX - width / 2f)) * (adjustedScaleFactor - 1)
            dy += (dy - (scaleDetector.focusY - height / 2f)) * (adjustedScaleFactor - 1)
        } else {
            lastScaleFactor = 0f
        }
        return true
    }

    override fun onScaleEnd(scaleDetector: ScaleGestureDetector) {}

    private fun applyBounds() {
        if (childCount == 0) return
        val child = getChildAt(0)

        val maxDx = max(0f, (child.width * scale - width) / 2f)
        val maxDy = max(0f, (child.height * scale - height) / 2f)

        dx = max(-maxDx, min(dx, maxDx))
        dy = max(-maxDy, min(dy, maxDy))
    }

    private fun applyScaleAndTranslation() {
        if (childCount == 0) return
        val child = getChildAt(0)
        child.scaleX = scale
        child.scaleY = scale
        child.pivotX = child.width / 2f
        child.pivotY = child.height / 2f
        child.translationX = dx
        child.translationY = dy
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
        applyScaleAndTranslation()
    }
}