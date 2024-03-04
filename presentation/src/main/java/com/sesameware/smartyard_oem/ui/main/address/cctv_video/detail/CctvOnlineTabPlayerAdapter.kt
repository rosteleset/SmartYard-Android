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
    private var isFullscreen = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CctvOnlineTabPlayerViewHolder {
        binding = ItemCctvDetailOnlinePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val isLandscape =
            (parent.context as AppCompatActivity).resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val parentWindowedWidth = if (isLandscape) parent.measuredHeight else parent.measuredWidth
        val windowedWidth = (parentWindowedWidth * ITEM_TO_PARENT_WIDTH_RATIO).roundToInt()
        return CctvOnlineTabPlayerViewHolder(binding, windowedWidth, onAction)
    }

    override fun getItemCount(): Int = previewUrls.size

    override fun onBindViewHolder(holder: CctvOnlineTabPlayerViewHolder, position: Int) {
        holder.bind(isFullscreen, previewUrls[position])
    }

    fun setFullscreen(isFullscreen: Boolean, currentPos: Int) {
        this.isFullscreen = isFullscreen
        // notify all items except current
        notifyItemRangeChanged(0, currentPos)
        notifyItemRangeChanged(currentPos + 1, previewUrls.size - currentPos - 1)
    }

    companion object {
        const val ITEM_TO_PARENT_WIDTH_RATIO = 0.9
    }
}