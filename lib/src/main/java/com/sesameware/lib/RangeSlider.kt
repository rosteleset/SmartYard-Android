package com.sesameware.lib

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.lib.databinding.RangeSliderBinding

class RangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var _binding: RangeSliderBinding? = null
    private val binding get() = _binding!!

    var slider: RangeSliderView private set
    private val itemCount = 5
    private val itemWidth: Int get() = measuredWidth / itemCount
    private var mMaskImages: HashBitmap? = null

    init {
        _binding = RangeSliderBinding.inflate(LayoutInflater.from(context), this, true)
        val array = context.obtainStyledAttributes(attrs, R.styleable.RangeSlider, 0, 0)
        val sliderHeight = array.getDimensionPixelOffset(
            R.styleable.RangeSlider_sliderHeight,
            resources.getDimensionPixelSize(R.dimen.default_slider_height)
        )
        slider = createSlider(context, array, binding.overlayView).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        binding.sliderView.apply {
            clipToOutline = true
            addView(slider, 2)
            layoutParams.height = sliderHeight
        }

        val bubbleHeight = resources.getDimensionPixelSize(R.dimen.bubble_height)
        val bubbleMargin = resources.getDimensionPixelSize(R.dimen.bubble_bottom_margin)
        val bubbleOffset = resources.getDimensionPixelSize(R.dimen.bubble_between_margin)
        binding.mainView.layoutParams.height = sliderHeight + bubbleHeight * 2 + bubbleMargin + bubbleOffset

        setupRV(context)
        array.recycle()
    }

    fun setupRV(context: Context) {
        binding.rvImageMask.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.rvImageMask.adapter = ThumbnailRecyclerView(itemCount)
    }

    inner class ThumbnailRecyclerView(private val count: Int) : RecyclerView.Adapter<ThumbnailViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ThumbnailViewHolder {
            return ThumbnailViewHolder(
                ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(itemWidth, MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            )
        }
        override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
            holder.setImage()
        }

        override fun getItemCount(): Int {
            return count
        }
    }

    inner class ThumbnailViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun setImage() {
            val mask = mMaskImages?.get(absoluteAdapterPosition)
            mask?.let {
                (itemView as ImageView).setImageBitmap(it)
            }
        }
    }

    private fun createSlider(
        context: Context,
        array: TypedArray,
        overlayView: OverlayView
    ): RangeSliderView {
        return RangeSliderView(
            context,
            overlayView,
            RangeSliderView.RangeSliderSettings(
                bgPaint = Paint().apply {
                    color = array.getColor(
                        R.styleable.RangeSlider_maskColor,
                        DEFAULT_MASK_BACKGROUND
                    )
                },
                trimThumbDrawable = array.getDrawable(R.styleable.RangeSlider_trimThumbDrawable) ?: ColorDrawable(DEFAULT_LINE_COLOR),
                seekThumbDrawable = array.getDrawable(R.styleable.RangeSlider_playThumbDrawable) ?: ColorDrawable(DEFAULT_LINE_COLOR),
                touchSlop = ViewConfiguration.get(context).scaledTouchSlop,
                tickCount = 500,
                trimMode = array.getBoolean(R.styleable.RangeSlider_trimMode, false)
            )
        )
    }

    fun setMaskImages(images: HashBitmap) {
        mMaskImages = images
        binding.rvImageMask.adapter?.notifyDataSetChanged()
    }

    fun setAvailableIntervals(timeInterval: TimeInterval?, intervals: List<TimeInterval>) {
        binding.intervalsBar.setAvailableIntervals(timeInterval, intervals)
    }

    fun setBarHeight(barHeight: Int) {
        binding.intervalsBar.setBarHeight(barHeight)
    }

    companion object {
        const val DEFAULT_MASK_BACKGROUND = -0x60000000
        const val DEFAULT_LINE_COLOR = -0x1000000
    }
}
