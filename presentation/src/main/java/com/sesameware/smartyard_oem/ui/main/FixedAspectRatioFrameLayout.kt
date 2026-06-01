package com.sesameware.smartyard_oem.ui.main

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.sesameware.smartyard_oem.R

class FixedAspectRatioFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var aspectRatio: Float = 0f
    private var dominantDimension: Int = DOMINANT_AUTO

    companion object {
        const val DOMINANT_WIDTH = 0
        const val DOMINANT_HEIGHT = 1
        const val DOMINANT_AUTO = 2
    }

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.FixedAspectRatioFrameLayout)
            try {
                val ratioStr = typedArray.getString(R.styleable.FixedAspectRatioFrameLayout_aspectRatio)
                aspectRatio = parseAspectRatio(ratioStr)
                dominantDimension = typedArray.getInt(
                    R.styleable.FixedAspectRatioFrameLayout_dominantDimension,
                    DOMINANT_AUTO
                )
            } finally {
                typedArray.recycle()
            }
        }
    }

    private fun parseAspectRatio(ratioStr: String?): Float {
        if (ratioStr.isNullOrEmpty()) return 0f

        return if (ratioStr.contains(":")) {
            val parts = ratioStr.split(":")
            if (parts.size == 2) {
                try {
                    val w = parts[0].trim().toFloat()
                    val h = parts[1].trim().toFloat()
                    if (w > 0f && h > 0f) w / h else 0f
                } catch (e: NumberFormatException) {
                    0f
                }
            } else {
                0f
            }
        } else {
            ratioStr.toFloatOrNull() ?: 0f
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (aspectRatio <= 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val useWidthAsBase = when (dominantDimension) {
            DOMINANT_WIDTH -> true
            DOMINANT_HEIGHT -> false
            else -> {
                if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                    true
                } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
                    false
                } else {
                    true
                }
            }
        }

        if (useWidthAsBase && widthMode == MeasureSpec.EXACTLY) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width / aspectRatio).toInt()
            val newHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, newHeightSpec)
        } else if (!useWidthAsBase && heightMode == MeasureSpec.EXACTLY) {
            val height = MeasureSpec.getSize(heightMeasureSpec)
            val width = (height * aspectRatio).toInt()
            val newWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            super.onMeasure(newWidthSpec, heightMeasureSpec)
        } else {
            // Если размеры не зафиксированы жестко (например, wrap_content), 
            // сначала измеряем детей стандартным образом
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            var resolvedWidth = measuredWidth
            var resolvedHeight = measuredHeight

            if (useWidthAsBase) {
                resolvedHeight = (resolvedWidth / aspectRatio).toInt()
            } else {
                resolvedWidth = (resolvedHeight * aspectRatio).toInt()
            }

            // Перезапускаем измерение с точными рассчитанными размерами
            val finalWidthSpec = MeasureSpec.makeMeasureSpec(resolvedWidth, MeasureSpec.EXACTLY)
            val finalHeightSpec = MeasureSpec.makeMeasureSpec(resolvedHeight, MeasureSpec.EXACTLY)
            super.onMeasure(finalWidthSpec, finalHeightSpec)
        }
    }

    fun setAspectRatio(ratio: Float) {
        if (this.aspectRatio != ratio) {
            this.aspectRatio = ratio
            requestLayout()
        }
    }

    fun setAspectRatio(width: Float, height: Float) {
        if (width > 0 && height > 0) {
            setAspectRatio(width / height)
        }
    }
}