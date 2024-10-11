package ru.madbrains.smartyard.ui.main.address.event_log.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.domain.model.response.Plog
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.setTextColorRes

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

        private fun getEventTypeResource(eType: Int): Int {
            return when (eType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> R.drawable.ic_eld_unanswered_call
                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> R.drawable.ic_eld_answered_call
                Plog.EVENT_OPEN_BY_KEY -> R.drawable.ic_el_key
                Plog.EVENT_OPEN_FROM_APP -> R.drawable.ic_el_app
                Plog.EVENT_OPEN_BY_FACE -> R.drawable.ic_el_face
                Plog.EVENT_OPEN_BY_CODE -> R.drawable.ic_el_code
                Plog.EVENT_OPEN_GATES_BY_CALL -> R.drawable.ic_el_gates
                Plog.EVENT_OPEN_BY_LINK -> R.drawable.ic_el_app
                else -> android.R.color.transparent
            }
        }

        private fun getEventTypeDescription(eType: Int): String {
            return when (eType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> "Звонок в домофон"
                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> "Звонок в домофон"
                Plog.EVENT_OPEN_BY_KEY -> "Открытие ключом"
                Plog.EVENT_OPEN_FROM_APP -> "Открытие из приложения"
                Plog.EVENT_OPEN_BY_FACE -> "Открытие по лицу"
                Plog.EVENT_OPEN_BY_CODE -> "Открытие по коду"
                Plog.EVENT_OPEN_GATES_BY_CALL -> "Открытие ворот звонком"
                Plog.EVENT_OPEN_BY_LINK -> "Открытие по ссылке"
                else -> "Неизвестное событие"
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
