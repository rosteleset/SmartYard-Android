package com.sesameware.smartyard_oem.ui.reg.providers

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.sesameware.smartyard_oem.R
import timber.log.Timber

class ProvidersAdapterDelegate(
    var activity: Activity,
    private val clickListener: (id: String, baseUrl: String) -> Unit
) : AdapterDelegate<List<ProviderModel>>() {
    private var checkedPosition = -1
    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<ProviderModel>, position: Int): Boolean {
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return ProviderItemViewHolder(inflater.inflate(R.layout.item_provider, parent, false))
    }

    override fun onBindViewHolder(
        items: List<ProviderModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as ProviderItemViewHolder
        val providerItem = items[position]
        vh.apply {
            tvTitle.text = providerItem.name
            checkbox.isClickable = false
            checkbox.isChecked = providerItem.isChecked
            holder.itemView.setOnClickListener {
                if (checkedPosition >= 0 && checkedPosition != position) {
                    items[checkedPosition].isChecked = false
                    bindingAdapter?.notifyItemChanged(checkedPosition)
                }
                checkbox.isChecked = true
                checkedPosition = position
                clickListener.invoke(providerItem.id, providerItem.baseUrl)
            }
        }
    }

    internal class ProviderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
}
