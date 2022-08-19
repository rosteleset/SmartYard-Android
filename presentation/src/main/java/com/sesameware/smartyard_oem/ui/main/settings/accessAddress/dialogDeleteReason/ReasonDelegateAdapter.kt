package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogDeleteReason

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class ReasonDelegateAdapter : AdapterDelegate<List<ReasonModel>>() {

    override fun isForViewType(items: List<ReasonModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_reason, parent, false)
        return VideoCameraViewHolder(view)
    }

    override fun onBindViewHolder(
        items: List<ReasonModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as VideoCameraViewHolder
        val reasonModel: ReasonModel = items[position]
        vh.tvTitle.text = reasonModel.name
        vh.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            items[position].check = isChecked
        }
    }

    internal class VideoCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var tvTitle: TextView = itemView.findViewById<View>(R.id.tvTitle) as TextView
        var checkBox: CheckBox = itemView.findViewById<View>(R.id.checkBox) as CheckBox
    }
}
