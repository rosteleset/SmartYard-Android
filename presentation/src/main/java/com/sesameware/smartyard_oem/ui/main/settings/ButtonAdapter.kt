package com.sesameware.smartyard_oem.ui.main.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemButtonBinding

class ButtonAdapter(private val onClick: () -> Unit) : RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder>() {

    class ButtonViewHolder(val button: Button) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val binding = ItemButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ButtonViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.button.setOnClickListener { onClick() }
    }

    override fun getItemCount(): Int = 1

    override fun getItemViewType(position: Int): Int {
        return R.layout.item_button
    }
}