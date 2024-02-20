package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.databinding.ItemCctvOnlineButtonsPageBinding
import kotlin.math.roundToInt

class CctvOnlineButtonsAdapter(
    rowsPerPage: Int,
    buttonsPerRow: Int,
    allButtonCount: Int,
    lastSelectedIndex: Int,
    private val onButtonClick: (Int) -> Unit
) : RecyclerView.Adapter<CctvOnlineButtonsViewHolder>() {
    private val rowsPerPage: Int = if (rowsPerPage > 0 ) rowsPerPage else
        throw IllegalArgumentException("rowsPerPage must be greater than 0")
    private val buttonsPerRow: Int = if (buttonsPerRow > 0 ) buttonsPerRow else
        throw IllegalArgumentException("buttonsPerRow must be greater than 0")
    private val allButtonCount: Int = if (allButtonCount > 0 ) allButtonCount else
        throw IllegalArgumentException("buttonCount must be greater than 0")
    private var lastSelectedIndex: Int = if (lastSelectedIndex > -1 ) lastSelectedIndex else
        throw IllegalArgumentException("lastSelectedIndex must be greater than -1")

    private val buttonsPerPage get() = rowsPerPage * buttonsPerRow
    private lateinit var binding: ItemCctvOnlineButtonsPageBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CctvOnlineButtonsViewHolder {
        binding = ItemCctvOnlineButtonsPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams.width = (parent.measuredWidth * ITEM_TO_PARENT_WIDTH_RATIO).roundToInt()
        return CctvOnlineButtonsViewHolder(binding, buttonsPerRow, ::onPageButtonClick)
    }

    override fun getItemCount(): Int = allButtonCount / buttonsPerPage + 1

    override fun onBindViewHolder(holder: CctvOnlineButtonsViewHolder, position: Int) {
        val firstIndex = buttonsPerPage * position
        val buttonsCount = if (position == itemCount - 1) {
            allButtonCount % buttonsPerPage
        } else {
            buttonsPerPage
        }
        val lastIndex = firstIndex + buttonsCount - 1
        holder.bind(firstIndex, lastIndex, lastSelectedIndex)
    }

    private fun onPageButtonClick(index: Int) {
        notifyItems(index)
        onButtonClick(index)
    }

    private fun notifyItems(index: Int): Int {
        lastSelectedIndex = index
        val pageToSelect = index / buttonsPerPage
        notifyItemRangeChanged(0, itemCount)
        return pageToSelect
    }

    fun selectButton(index: Int): Int {
        return notifyItems(index)
    }

    companion object {
        const val ITEM_TO_PARENT_WIDTH_RATIO = 0.8
    }
}