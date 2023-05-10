package ru.madbrains.lib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import org.threeten.bp.LocalDateTime

/**
 * Create On 16/10/2016
 * @author wayne
 */
@SuppressLint("ViewConstructor")
class ThumbView(
    context: Context,
    val bubbleView: BubbleView,
    private val mThumbDrawable: Drawable,
    private val count: Int,
    val selectable: Boolean,
    val initialRangeIndex: Int
) : View(context) {

    var currentTime: LocalDateTime = LocalDateTime.now()
    private val mExtendTouchSlop: Int
    private var mPressed = false
    var rangeIndex = initialRangeIndex
    val mWidth get() = mThumbDrawable.intrinsicWidth

    init {
        mExtendTouchSlop = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            EXTEND_TOUCH_SLOP.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        background = mThumbDrawable
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY)
        )
        mThumbDrawable.setBounds(0, 0, mWidth, measuredHeight)
    }

    fun inInTarget(x: Int, y: Int): Boolean {
        val rect = Rect()
        getHitRect(rect)
        rect.left -= mExtendTouchSlop
        rect.right += mExtendTouchSlop
        rect.top -= mExtendTouchSlop
        rect.bottom += mExtendTouchSlop
        return rect.contains(x, y)
    }

    fun show(active: Boolean) {
        this.visible(active)
        bubbleView.visible(active)
    }

    override fun isPressed(): Boolean {
        return mPressed
    }

    override fun setPressed(pressed: Boolean) {
        mPressed = pressed
    }

    fun getProgress(): Double {
        return rangeIndex.toDouble() / count
    }

    companion object {
        private const val EXTEND_TOUCH_SLOP = 15
    }
}
