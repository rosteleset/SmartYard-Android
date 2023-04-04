package com.sesameware.smartyard_oem.ui.main.burger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.databinding.ItemBurgerBinding

class BurgerDelegate: AdapterDelegate<List<BurgerModel>>() {
    override fun isForViewType(items: List<BurgerModel>, position: Int): Boolean {
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val itemBinding = ItemBurgerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BurgerViewHolder(itemBinding)
    }

    override fun onBindViewHolder(
        items: List<BurgerModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val binding = ItemBurgerBinding.bind(holder.itemView)
        if (items[position].iconId != null) {
            Glide.with(binding.ivIcon)
                .load(items[position].iconId)
                .into(binding.ivIcon)
        } else if (items[position].iconUrl != null) {
            Glide.with(binding.ivIcon)
                .load(items[position].iconUrl)
                .into(binding.ivIcon)
        }

        if (items[position].titleId != null) {
            binding.tvItem.text = holder.itemView.context.getString(items[position].titleId!!)
        } else {
            binding.tvItem.text = items[position].title
        }
        binding.root.setOnClickListener {
            items[position].onClick.invoke()
        }
    }

    internal class BurgerViewHolder(itemBinding: ItemBurgerBinding) : RecyclerView.ViewHolder(itemBinding.root)
}
