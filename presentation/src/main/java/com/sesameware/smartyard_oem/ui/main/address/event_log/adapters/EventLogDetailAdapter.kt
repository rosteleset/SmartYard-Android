package com.sesameware.smartyard_oem.ui.main.address.event_log.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.LocalDate
import com.sesameware.domain.model.response.Plog
import com.sesameware.smartyard_oem.databinding.ItemEventLogDetailBinding
import kotlin.math.roundToInt

class EventLogDetailAdapter(
    var eventsDay: List<LocalDate>,
    var eventsByDays: HashMap<LocalDate, MutableList<Plog>>,
    private val onAction: (EventLogDetailItemAction) -> Unit
) : RecyclerView.Adapter<EventLogDetailVH>() {

    private lateinit var binding: ItemEventLogDetailBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventLogDetailVH {
        binding = ItemEventLogDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams.width = (parent.measuredWidth * ITEM_WIDTH_TO_RECYCLERVIEW_WIDTH_RATIO).roundToInt()
        return EventLogDetailVH(binding, onAction)
    }

    override fun onBindViewHolder(holder: EventLogDetailVH, position: Int) {
        val (day, index) = getPlog(position)
        if (day == null || index == null) return
        val plog = eventsByDays[day]?.get(index)
        holder.onBind(position, plog!!)
    }

    override fun getItemCount(): Int {
        var count = 0
        eventsByDays.keys.forEach { day ->
            count += eventsByDays[day]?.size ?: 0
        }

        return count
    }

    fun getPlog(position: Int): Pair<LocalDate?, Int?> {
        if (position < 0) {
            return Pair(null, null)
        }

        var index = 0
        eventsDay.forEach { day ->
            val size = eventsByDays[day]?.size ?: 0
            if (index + size <= position) {
                index += size
            } else {
                val k = position - index
                return Pair(day, k)
            }
        }

        return Pair(null, null)
    }

    companion object {
        private const val ITEM_WIDTH_TO_RECYCLERVIEW_WIDTH_RATIO = 0.9
    }
}