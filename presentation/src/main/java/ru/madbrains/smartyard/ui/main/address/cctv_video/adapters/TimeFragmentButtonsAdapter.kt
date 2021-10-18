package ru.madbrains.smartyard.ui.main.address.cctv_video.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.LocalDate
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.lib.TimeInterval
import ru.madbrains.lib.TimeInterval.Companion.INTERVAL_STEP_
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.cctv_video.AvailableRange

class TimeFragmentButtonsAdapter(
    private val chosenDate: LocalDate,
    private val availableRanges: List<AvailableRange>,
    private val mCallback: listenerGeneric<TimeInterval>,
    private val listenerPosition: (position: Int) -> Unit
) : RecyclerView.Adapter<TimeFragmentButtonsAdapter.TimeFragmentButtonsViewHolder>() {

    private var currentPos: Int? = null
    private val mItems: List<TimeInterval> = generateTimeIntervals()
    private var isFullscreen = false

    private fun generateTimeIntervals(): List<TimeInterval> {
        val result = mutableListOf<TimeInterval>()
        var startRange = chosenDate.atStartOfDay()
        val endRange = startRange.plusDays(1)
        while (startRange < endRange) {
            val rangeFrom = startRange
            var rangeTo = rangeFrom.plusHours(INTERVAL_STEP_)

            //интервал вида 21.00 - 00.00 показываем как 21.00 - 23.59
            if (rangeTo.hour == 0 && rangeTo.minute == 0 && rangeTo.second == 0) {
                rangeTo = rangeTo.minusSeconds(1)
            }

            var leftMargin = rangeTo
            var rightMargin = rangeFrom

            //ищем пересечения с интервалами из архива
            availableRanges.forEach {range ->
                if ((rangeFrom < range.endDate) && (range.startDate < rangeTo)) {
                    val intersectionLeft = if (rangeFrom > range.startDate) rangeFrom else range.startDate
                    val intersectionRight = if (rangeTo < range.endDate) rangeTo else range.endDate
                    if (intersectionLeft < leftMargin) {
                        leftMargin = intersectionLeft
                    }
                    if (intersectionRight > rightMargin) {
                        rightMargin = intersectionRight
                    }
                }
            }

            if (leftMargin < rightMargin) {
                //есть пересечение с интервалами из архива
                result.add(TimeInterval(leftMargin, rightMargin))
            }

            startRange = rangeFrom.plusHours(INTERVAL_STEP_)
        }

        return result
    }

    val adapter = this
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeFragmentButtonsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cctv_time_fragment_button, parent, false)
        return TimeFragmentButtonsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeFragmentButtonsViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun select(position: Int) {
        if (0 <= position && position < mItems.size) {
            mCallback(mItems[position])
            listenerPosition.invoke(position)
            currentPos = position
            adapter.notifyDataSetChanged()
        }
    }

    fun setFullscreen(value: Boolean) {
        isFullscreen = value
        adapter.notifyDataSetChanged()
    }

    inner class TimeFragmentButtonsViewHolder constructor(itemView: View) : RecyclerView.ViewHolder
    (itemView) {
        private val buttonView: LinearLayout = itemView.findViewById(R.id.buttonView)
        private val textView: TextView = itemView.findViewById(R.id.tvTimeText)
        private val tf = textView.typeface

        fun onBind(position: Int) {
            val lp = buttonView.layoutParams as ViewGroup.MarginLayoutParams
            if (position == 0) {
                lp.marginStart = itemView.context.resources.getDimensionPixelSize(R.dimen.cctv_trim_page_margin)
            } else {
                lp.marginStart = 0
            }
            textView.text = mItems[position].intervalText
            textView.setTextColor(ContextCompat.getColor(textView.context, if (isFullscreen) R.color.white_0 else R.color.grey_100))
            textView.setSel(position == currentPos)
            buttonView.setOnClickListener { select(position) }
        }

        private fun TextView.setSel(selected: Boolean) {
            this.isSelected = selected
            this.setBackgroundResource(if (isFullscreen) R.drawable.cctv_interval_selected_fs else R.drawable.cctv_interval_selected)
            this.setTypeface(tf, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
