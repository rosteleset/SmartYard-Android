package com.sesameware.smartyard_oem.ui.main.address.event_log.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.domain.model.response.Plog
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.setTextColorRes

class EventLogChildAdapter(
    private val eventsByDay: List<Plog>,
    private val eventCallback: listenerGeneric<Int>
) : RecyclerView.Adapter<EventLogChildAdapter.EventLogChildAdapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup,
        viewType: Int): EventLogChildAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_log_child, parent, false)
        return EventLogChildAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventLogChildAdapterViewHolder, position: Int) {
        holder.onBind(position)
        holder.itemView.setOnClickListener {
            eventCallback(position)
        }
    }

    override fun getItemCount(): Int {
        return eventsByDay.size
    }

    inner class EventLogChildAdapterViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivType: ImageView = itemView.findViewById(R.id.ivEventLogChildType)
        private val tvType: TextView = itemView.findViewById(R.id.tvEventLogChildType)
        private val tvTime: TextView = itemView.findViewById(R.id.tvEventLogChildTime)
        private val vLine: View = itemView.findViewById(R.id.vEventLogChildLine)

        private fun getEventTypeResource(@Plog.IntercomEvent eType: Int): Int {
            return when (eType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> R.drawable.ic_el_call_doorphone
                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> R.drawable.ic_el_call_doorphone
                Plog.EVENT_OPEN_BY_KEY -> R.drawable.ic_el_key
                Plog.EVENT_OPEN_FROM_APP -> R.drawable.ic_el_app
                Plog.EVENT_OPEN_BY_FACE -> R.drawable.ic_el_face
                Plog.EVENT_OPEN_BY_CODE -> R.drawable.ic_el_code
                Plog.EVENT_OPEN_GATES_BY_CALL -> R.drawable.ic_el_gates
                else -> android.R.color.transparent
            }
        }

        private fun getEventTypeDescription(@Plog.IntercomEvent eType: Int): String {
            return when (eType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> itemView.context.getString(R.string.event_door_phone_call_unanswered)
                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> itemView.context.getString(R.string.event_door_phone_call_answered)
                Plog.EVENT_OPEN_BY_KEY -> itemView.context.getString(R.string.event_open_by_key)
                Plog.EVENT_OPEN_FROM_APP -> itemView.context.getString(R.string.event_open_from_app)
                Plog.EVENT_OPEN_BY_FACE -> itemView.context.getString(R.string.event_open_by_face)
                Plog.EVENT_OPEN_BY_CODE -> itemView.context.getString(R.string.event_open_by_code)
                Plog.EVENT_OPEN_GATES_BY_CALL -> itemView.context.getString(R.string.event_open_gates_by_call)
                else -> itemView.context.getString(R.string.event_unknown)
            }
        }

        fun onBind(position: Int) {
            val eType = eventsByDay[position].eventType
            ivType.setImageResource(getEventTypeResource(eType))
            tvType.text = getEventTypeDescription(eType)
            tvType.setTextColorRes(if (eType == Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED) R.color.red_100 else R.color.black_200)
            tvTime.text = eventsByDay[position].date.format(DateTimeFormatter.ofPattern("HH:mm"))
            vLine.visibility = (if (position == eventsByDay.size - 1) View.INVISIBLE else View.VISIBLE)
        }
    }
}
