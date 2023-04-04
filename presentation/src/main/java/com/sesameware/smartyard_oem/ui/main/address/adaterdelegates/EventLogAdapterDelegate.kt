package com.sesameware.smartyard_oem.ui.main.address.adaterdelegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.adapters.ParentListAdapterSetting
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.EventLogModel

class EventLogAdapterDelegate(private val setting: ParentListAdapterSetting) :
    AdapterDelegate<List<DisplayableItem>>() {

    private val inflater: LayoutInflater = LayoutInflater.from(setting.context)

    override fun isForViewType(items: List<DisplayableItem>, position: Int): Boolean {
        return (items[position] is EventLogModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return EventLogViewHolder(inflater.inflate(R.layout.item_event_log, parent, false))
    }

    override fun onBindViewHolder(items: List<DisplayableItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as EventLogViewHolder
        val eventLog: EventLogModel = items[position] as EventLogModel
        if (eventLog.resourceId != null) {
            vh.caption.text = vh.caption.context.getString(eventLog.resourceId!!)
        } else {
            vh.caption.text = eventLog.caption
        }
        vh.count.text = eventLog.counter.toString()
        vh.itemView.setOnClickListener {setting.clickEventLog(eventLog)}
    }

    internal class EventLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var caption: TextView = itemView.findViewById(R.id.tvTitleEL)
        var count: TextView = itemView.findViewById(R.id.tvCountEL)
    }
}
