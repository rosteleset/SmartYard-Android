package ru.madbrains.lib

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import ru.madbrains.lib.databinding.BubbleViewBinding

/**
 * Create On 16/10/2016
 * @author wayne
 */
@SuppressLint("ViewConstructor")
class BubbleView(context: Context) : LinearLayout(context) {
    private var _binding: BubbleViewBinding? = null
    private val binding get() = _binding!!

    init {
        _binding = BubbleViewBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
    }
    private val overlayView: OverlayView get() = parent as OverlayView

    fun moveX(x: Float) {
        waitForMeasure {
            val half = measuredWidth / 2
            val offset = x - half
            levelPos(offset)
        }
    }

    private fun levelPos(x: Float) {
        this.x = x.coerceIn(0f, overlayView.width.toFloat() - measuredWidth)
    }

    fun setTimeText(text: String) {
        if (text.length > binding.tvTime.text.length) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    levelPos(x)
                }
            })
        }
        binding.tvTime.text = text
    }
}
