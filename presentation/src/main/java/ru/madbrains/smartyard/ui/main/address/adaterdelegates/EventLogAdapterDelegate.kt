package ru.madbrains.smartyard.ui.main.address.adaterdelegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapterSetting
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.EventLogModel

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
        vh.caption.text = eventLog.caption
        vh.count.text = eventLog.counter.toString()
        vh.itemView.setOnClickListener {setting.clickEventLog(eventLog)}
    }

    internal class EventLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var caption: TextView = itemView.findViewById(R.id.tvTitleEL)
        var count: TextView = itemView.findViewById(R.id.tvCountEL)
    }
}
