package com.sesameware.lib

import android.content.res.Resources
import android.view.View
import android.view.ViewTreeObserver
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDateTime

fun Int.dpToPx(): Int {
    return dpToPxF().toInt()
}
fun Int.dpToPxF(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun View.waitForMeasure(callback: (View) -> Unit) {
    val view = this
    if (width > 0) {
        callback.invoke(view)
    } else if (viewTreeObserver.isAlive) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                callback.invoke(view)
            }
        })
    }
}

fun View.visible(on: Boolean, invisible: Boolean = false) {
    this.visibility = if (on) View.VISIBLE else if (invisible) View.INVISIBLE else View.GONE
}
fun LocalDateTime.timeInMs(): Long {
    return DateTimeUtils.toSqlTimestamp(this).time
}
fun LocalDateTime.toTimeStamp(): Long {
    return this.timeInMs() / 1000
}
