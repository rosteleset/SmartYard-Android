package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import org.threeten.bp.LocalDate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.setTextColorRes
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.AvailableRange
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.isDateInAvailableRanges

class DayViewContainer(view: View) : ViewContainer(view) {
    lateinit var day: CalendarDay
    val dateNum: TextView = view.findViewById(R.id.dateNum)
}
class CalendarDayBinder(
    private val today: LocalDate,
    private val rangeDays: ClosedRange<LocalDate>,
    private val availableRanges: List<AvailableRange>,
    private val notifyDate: (oldDate: LocalDate?, newDate: LocalDate) -> Unit,
    private val clickOnDate: (date: LocalDate) -> Unit
) : DayBinder<DayViewContainer> {
    private var selectedDate: LocalDate? = null

    override fun create(view: View) =
        DayViewContainer(view)

    override fun bind(container: DayViewContainer, day: CalendarDay) {
        container.day = day
        val dateNum = container.dateNum
        dateNum.text = day.date.dayOfMonth.toString()
        dateNum.setOnClickListener {
            clickOnDate(day.date)
        }

        if (day.owner == DayOwner.THIS_MONTH) {
            dateNum.isVisible = true
            when (day.date) {
//                today -> {
//                    dateNum.setTextColorRes(R.color.calendar_white)
//                    dateNum.setBackgroundResource(R.drawable.calendar_today_bg)
//                }
                selectedDate -> {
                    dateNum.setTextColorRes(R.color.calendar_blue)
                    dateNum.setBackgroundResource(R.drawable.calendar_selected_bg)
                }
                else -> {
                    if (rangeDays.contains(day.date) && isDateInAvailableRanges(day.date, availableRanges)) {
                        dateNum.setTextColorRes(R.color.calendar_black)
                    } else {
                        dateNum.setTextColorRes(R.color.calendar_grey)
                    }
                    dateNum.background = null
                }
            }
        } else {
            dateNum.isVisible = false
        }
    }

    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            notifyDate(oldDate, date)
        }
    }
}
