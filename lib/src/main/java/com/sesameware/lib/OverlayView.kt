package com.sesameware.lib

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var animationY: ObjectAnimator? = null
    private val bubbleHeight = resources.getDimensionPixelSize(R.dimen.bubble_height)
    private val bubbleBetweenMargin = resources.getDimensionPixelSize(R.dimen.bubble_between_margin)
    private val bubbleNormalY: Float get() = (bubbleHeight + bubbleBetweenMargin).toFloat()
    val bubble1 = initBubble(context)
    val bubble2 = initBubble(context)
    val bubbleSeek = initBubble(context)

    init {
        addView(bubble1)
        addView(bubble2)
        addView(bubbleSeek)
        setWillNotDraw(false)
    }

    fun checkAnimateRightBubble() {
        bubble1.waitForMeasure {
            val toY = if (bubble1.x + it.measuredWidth + 5 > bubble2.x) 0f else bubbleNormalY
            if (toY != bubble2.y && animationY?.isStarted != true) {
                animationY = ObjectAnimator.ofFloat(bubble2, "translationY", toY).apply {
                    duration = 100
                    start()
                }
            }
        }
    }

    private fun initBubble(context: Context): BubbleView {
        return BubbleView(context).apply {
            y = bubbleNormalY
        }
    }
}
