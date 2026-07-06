package com.sesameware.smartyard_oem.ui.main.address.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragToSortCallback(
    private val onItemsSwap: (Int, Int) -> Unit,
    private val onItemDrag: (RecyclerView.ViewHolder?) -> Unit,
    private val onItemRelease: (RecyclerView.ViewHolder?) -> Unit
) : ItemTouchHelper.Callback() {

    private var lastDraggedItem: RecyclerView.ViewHolder? = null

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            onItemDrag(viewHolder)
            lastDraggedItem = viewHolder
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            onItemRelease(lastDraggedItem)
            lastDraggedItem = null
        }
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun chooseDropTarget(
        selected: RecyclerView.ViewHolder,
        dropTargets: MutableList<RecyclerView.ViewHolder>,
        curX: Int,
        curY: Int
    ): RecyclerView.ViewHolder? {
        val selectedBottom = curY + selected.itemView.height
        val selectedTop = curY

        for (target in dropTargets) {
            val targetCenter = target.itemView.top + target.itemView.height / 2
            val selectedPosition = selected.bindingAdapterPosition
            val targetPosition = target.bindingAdapterPosition

            if (targetPosition > selectedPosition) {
                if (selectedBottom > targetCenter) return target
            } else if (targetPosition < selectedPosition) {
                if (selectedTop < targetCenter) return target
            }
        }

        return null
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition

        if (fromPosition == RecyclerView.NO_POSITION ||
            toPosition == RecyclerView.NO_POSITION) return false

        onItemsSwap(fromPosition, toPosition)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun isLongPressDragEnabled(): Boolean = false
    override fun isItemViewSwipeEnabled(): Boolean = false
}