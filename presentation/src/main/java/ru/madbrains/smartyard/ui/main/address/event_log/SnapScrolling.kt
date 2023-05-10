package ru.madbrains.smartyard.ui.main.address.event_log

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

fun SnapHelper.getSnapPosition(recyclerView: RecyclerView): Int {
    val layoutManager = recyclerView.layoutManager ?: return RecyclerView.NO_POSITION
    val snapView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
    return layoutManager.getPosition(snapView)
}

interface OnSnapPositionChangeListener {
    fun onSnapPositionChanged(prevPosition: Int, newPosition: Int)
}

class SnapOnScrollListener(
    private var snapHelper: SnapHelper,
    var behavior: ScrollBehavior = ScrollBehavior.NOTIFY_ON_SCROLL_IDLE,
    var onSnapPositionChangeListener: OnSnapPositionChangeListener? = null
) : RecyclerView.OnScrollListener() {
    
    enum class ScrollBehavior {
        NOTIFY_ON_SCROLL,
        NOTIFY_ON_SCROLL_IDLE
    }

    private var snapPosition = RecyclerView.NO_POSITION

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (behavior == ScrollBehavior.NOTIFY_ON_SCROLL || dx == 0 && dy == 0) {
            maybeNotifySnapPositionChanged(recyclerView)
        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (behavior == ScrollBehavior.NOTIFY_ON_SCROLL_IDLE
            && newState == RecyclerView.SCROLL_STATE_IDLE) {
            maybeNotifySnapPositionChanged(recyclerView)
        }
    }

    private fun maybeNotifySnapPositionChanged(recycleView: RecyclerView) {
        val newSnapPosition = snapHelper.getSnapPosition(recycleView)
        val snapPositionChanged = snapPosition != newSnapPosition
        if (snapPositionChanged) {
            onSnapPositionChangeListener?.onSnapPositionChanged(snapPosition, newSnapPosition)
            snapPosition = newSnapPosition
        }
    }
}

fun RecyclerView.attachSnapHelperWithListener(
    snapHelper: SnapHelper,
    behavior: SnapOnScrollListener.ScrollBehavior = SnapOnScrollListener.ScrollBehavior.NOTIFY_ON_SCROLL_IDLE,
    onSnapPositionChangeListener: OnSnapPositionChangeListener
) {
    snapHelper.attachToRecyclerView(this)
    val snapOnScrollListener = SnapOnScrollListener(snapHelper, behavior, onSnapPositionChangeListener)
    addOnScrollListener(snapOnScrollListener)
}
