package com.sesameware.smartyard_oem.ui.main.player.log

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.text.set
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.PlayerLog

class PlayerLogAdapter(
    private val onShareLog: (Int?) -> Unit
) : ListAdapter<PlayerLog, PlayerLogAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val line = TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            val paddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                parent.context.resources.displayMetrics
            ).toInt()
            setPadding(paddingPx, 0, paddingPx, 0)

            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            setBackgroundResource(typedValue.resourceId)
        }

        return ViewHolder(line)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(getItem(position), position)
    }

    private fun String.paintBy(@ColorInt color: Int) =
        toSpannable().apply { this[0, this.length] = ForegroundColorSpan(color) }

    inner class ViewHolder(val line: TextView) : RecyclerView.ViewHolder(line) {
        fun bind(item: PlayerLog, index: Int) {
            val color = when (item.level) {
                "DEBUG" -> Color.GREEN
                "INFO" -> Color.WHITE
                "WARN" -> Color.YELLOW
                "ERROR" -> Color.RED
                else -> Color.MAGENTA
            }

            val spannedText = item.message.paintBy(color)

            line.text = spannedText

            line.setOnClickListener {
                onShareLog(index)
            }

            line.setOnLongClickListener {
                onShareLog(null)
                true
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlayerLog>() {
        override fun areItemsTheSame(
            oldItem: PlayerLog,
            newItem: PlayerLog
        ): Boolean = oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(
            oldItem: PlayerLog,
            newItem: PlayerLog
        ): Boolean = oldItem == newItem
    }
}