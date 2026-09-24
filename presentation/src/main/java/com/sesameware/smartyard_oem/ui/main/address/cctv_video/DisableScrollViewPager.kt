package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

// A-MOBILE - ADDED {{{
class DisableScrollViewPager(
    context: Context,
    attrs: AttributeSet?
) : ViewPager(context, attrs) {
    var scrollEnabled = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (this.scrollEnabled) {
            return super.onTouchEvent(event)
        }

        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (this.scrollEnabled) {
            return super.onInterceptTouchEvent(event)
        }

        return false
    }
}
// }}}