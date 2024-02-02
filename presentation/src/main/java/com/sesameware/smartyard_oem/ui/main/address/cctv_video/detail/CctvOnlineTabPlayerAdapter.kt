package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding
import kotlin.math.roundToInt

class CctvOnlineTabPlayerAdapter(
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit,
    private val previewUrls: List<String>
) : RecyclerView.Adapter<CctvOnlineTabPlayerVH>() {

    private lateinit var binding: ItemCctvDetailOnlinePlayerBinding
    private var isFullscreen = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CctvOnlineTabPlayerVH {
        binding = ItemCctvDetailOnlinePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val windowedWidth = (parent.measuredWidth * ITEM_WIDTH_TO_RECYCLERVIEW_WIDTH_RATIO).roundToInt()
        return CctvOnlineTabPlayerVH(binding, windowedWidth, onAction)
    }

    override fun getItemCount(): Int = previewUrls.size

    override fun onBindViewHolder(holder: CctvOnlineTabPlayerVH, position: Int) {
        holder.bind(isFullscreen, previewUrls[position])
    }

    fun setFullscreen(isFullscreen: Boolean, currentPos: Int) {
        this.isFullscreen = isFullscreen
        // notify all items except current
        notifyItemRangeChanged(0, currentPos)
        notifyItemRangeChanged(currentPos + 1, previewUrls.size - currentPos - 1)
    }

    companion object {
        private const val ITEM_WIDTH_TO_RECYCLERVIEW_WIDTH_RATIO = 0.9
    }
}