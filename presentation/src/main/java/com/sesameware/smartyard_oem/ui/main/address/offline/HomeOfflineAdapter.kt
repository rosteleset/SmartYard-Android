package com.sesameware.smartyard_oem.ui.main.address.offline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.OfflineAddress
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemOfflineAddressHeaderBinding
import com.sesameware.smartyard_oem.databinding.ItemOfflineDoorBinding
import com.sesameware.smartyard_oem.setTextColorRes

class HomeOfflineAdapter(
    private val onExpandClick: (Int, Boolean) -> Unit
) : ListAdapter<OfflineAddress, HomeOfflineAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfflineAddressHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemOfflineAddressHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OfflineAddress) {
            with (binding) {
                val hasDoors = item.doors.isNotEmpty()

                val colorRes = if (hasDoors) R.color.accent else R.color.disabled
                tvAddressTitle.setTextColorRes(colorRes)
                tvAddressTitle.text = item.address

                ivExpandHouse.isVisible = hasDoors
                ivExpandHouse.setOnClickListener {
                    ivExpandHouse.isSelected = !ivExpandHouse.isSelected
                    onExpandClick(bindingAdapterPosition, ivExpandHouse.isSelected)
                    expandableLayout.toggle()
                }
                ivExpandHouse.isSelected = item.isExpanded

                doorsContainer.removeAllViews()
                if (hasDoors) {
                    val inflater = LayoutInflater.from(root.context)
                    item.doors.forEach { door ->
                        val doorBinding =
                            ItemOfflineDoorBinding.inflate(inflater, doorsContainer, false)
                        doorBinding.tvDoorName.text = door.name
                        doorBinding.tvDoorCode.text = door.doorCode
                        doorsContainer.addView(doorBinding.root)
                    }
                }
                expandableLayout.setExpanded(item.isExpanded, false)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<OfflineAddress>() {
        override fun areItemsTheSame(oldItem: OfflineAddress, newItem: OfflineAddress): Boolean {
            return oldItem.houseId == newItem.houseId
        }

        override fun areContentsTheSame(oldItem: OfflineAddress, newItem: OfflineAddress): Boolean {
            return oldItem == newItem
        }
    }
}
