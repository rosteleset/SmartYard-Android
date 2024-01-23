package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailOnlinePlayerBinding

class CctvOnlineTabPlayerAdapter(
    private val onAction: (CctvOnlineTabPlayerAction) -> Unit,
    private val camerasCount: Int
) : RecyclerView.Adapter<CctvOnlineTabPlayerVH>() {

    private lateinit var binding: ItemCctvDetailOnlinePlayerBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CctvOnlineTabPlayerVH {
        binding = ItemCctvDetailOnlinePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CctvOnlineTabPlayerVH(binding, onAction)
    }

    override fun getItemCount(): Int = camerasCount

    override fun onBindViewHolder(holder: CctvOnlineTabPlayerVH, position: Int) {}
}