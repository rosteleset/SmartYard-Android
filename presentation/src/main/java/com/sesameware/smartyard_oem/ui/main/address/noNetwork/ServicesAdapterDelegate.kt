package com.sesameware.smartyard_oem.ui.main.address.noNetwork

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.noNetwork.NoNetworkFragment.ItemService

/**
 * @author Nail Shakurov
 * Created on 2020-02-21.
 */
class ServicesAdapterDelegate(
    val click: () -> Unit
) : AdapterDelegate<List<ItemService>>() {

    override fun isForViewType(items: List<ItemService>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_services, parent, false)
        return VideoCameraViewHolder(view)
    }

    override fun onBindViewHolder(
        items: List<ItemService>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        with(holder as VideoCameraViewHolder) {
            this.tvTitle.text = items[position].name
            this.checkBox.setOnClickListener { click.invoke() }
            this.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                items[position].check = isChecked
            }
        }
    }

    internal class VideoCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var tvTitle: TextView = itemView.findViewById<View>(R.id.tvTitle) as TextView
        var checkBox: CheckBox = itemView.findViewById<View>(R.id.checkBox) as CheckBox
    }
}
