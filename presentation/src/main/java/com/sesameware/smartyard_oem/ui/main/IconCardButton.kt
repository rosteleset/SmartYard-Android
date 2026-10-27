package com.sesameware.smartyard_oem.ui.main

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import com.sesameware.smartyard_oem.R

class IconCardButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val imageView: AppCompatImageView = AppCompatImageView(context)

    init {
        cardElevation = 0f
        maxCardElevation = 0f
        radius = dpToPx(50f)
        setCardBackgroundColor("#66000000".toColorInt())

        isClickable = true
        isFocusable = true
        clipToOutline = true

        runCatching {
            val typedValue = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                foreground = ContextCompat.getDrawable(context, typedValue.resourceId)
            }
        }

        val padding = dpToPx(8f).toInt()
        imageView.setPadding(padding, padding, padding, padding)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.isDuplicateParentStateEnabled = true

        val minSizePx = dpToPx(48f).toInt()
        minimumWidth = minSizePx
        minimumHeight = minSizePx

        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        addView(imageView, params)

        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.IconCardButton, defStyleAttr, 0) {
                var iconResId = getResourceId(R.styleable.IconCardButton_android_src, 0)
                if (iconResId == 0) {
                    iconResId = getResourceId(R.styleable.IconCardButton_srcCompat, 0)
                }
                if (iconResId != 0) {
                    setIconResource(iconResId)
                } else {
                    val drawable = getDrawable(R.styleable.IconCardButton_android_src)
                        ?: getDrawable(R.styleable.IconCardButton_srcCompat)
                    drawable?.let { setIcon(it) }
                }
            }
        }
    }

    fun setIconResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            imageView.setImageResource(resId)
        } else {
            imageView.setImageDrawable(null)
        }
    }

    fun setIcon(drawable: Drawable?) {
        imageView.setImageDrawable(drawable)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
}