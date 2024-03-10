package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import kotlin.math.roundToInt

class CctvOnlineTabPlayerAdapter(
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit,
    private val previewUrls: List<String>
) : RecyclerView.Adapter<CctvOnlineTabPlayerViewHolder>() {

    private lateinit var binding: ItemCctvDetailOnlinePlayerBinding
    var isFullscreen: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyAdjacentItems()
        }
    var isLandscape: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyAdjacentItems()
        }
    var currentPos: Int = -1
        set(value) {
            if (field == value) return
            val previousPos = field
            field = value
            notifyItemChanged(previousPos)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CctvOnlineTabPlayerViewHolder {
        binding = ItemCctvDetailOnlinePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val parentWindowedWidth = if (isLandscape) parent.measuredHeight else parent.measuredWidth
        val windowedWidth = (parentWindowedWidth * ITEM_TO_PARENT_WIDTH_RATIO).roundToInt()
        return CctvOnlineTabPlayerViewHolder(binding, windowedWidth, onAction)
    }

    override fun getItemCount(): Int = previewUrls.size

    override fun onBindViewHolder(holder: CctvOnlineTabPlayerViewHolder, position: Int) {
        holder.bind(isFullscreen, isLandscape, previewUrls[position])
    }

    // notify all items except current
    private fun notifyAdjacentItems() {
        notifyItemRangeChanged(0, currentPos)
        notifyItemRangeChanged(currentPos + 1, previewUrls.size - currentPos - 1)
    }

    companion object {
        const val ITEM_TO_PARENT_WIDTH_RATIO = 0.9
    }
}