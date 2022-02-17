package ru.madbrains.smartyard.ui.main.address.cctv_video.detail.arhive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.yearMonth
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentCctvDetailArchiveBinding
import ru.madbrains.smartyard.daysOfWeekFromLocale
import ru.madbrains.smartyard.setTextColorRes
import ru.madbrains.smartyard.ui.main.address.cctv_video.AvailableRange
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.detail.CalendarDayBinder
import ru.madbrains.smartyard.ui.main.address.cctv_video.isDateInAvailableRanges

class CCTVArchiveTab : Fragment() {
    private var _binding: FragmentCctvDetailArchiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private var availableRanges = mutableListOf<AvailableRange>()

    private lateinit var rangeDays: ClosedRange<LocalDate>
    private var currentYearMonth = YearMonth.now()
    private var minYearMonth = currentYearMonth
    private var maxYearMonth = currentYearMonth

    private val titleSameYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val titleFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    companion object {
        val key_startDate = "key_startDate"
        val key_endDate = "key_endDate"
        val key_availableRanges = "key_availableRanges"

        fun newInstance(
            startDate: LocalDate,
            endDate: LocalDate,
            availableRanges: List<AvailableRange>
        ) = CCTVArchiveTab().apply {
            arguments = Bundle().apply {
                putSerializable(key_startDate, startDate)
                putSerializable(key_endDate, endDate)
                putParcelableArray(key_availableRanges, availableRanges.toTypedArray())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  {
        _binding = FragmentCctvDetailArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun scrollToMonth(plus: Boolean) {
        currentYearMonth = if (plus) {
            currentYearMonth.plusMonths(1)
        } else {
            currentYearMonth.minusMonths(1)
        }

        binding.archiveCalendar.scrollToMonth(currentYearMonth)
    }

    private fun updateModButtons(yearMonth: YearMonth) {
        if (yearMonth <= minYearMonth) {
            binding.monthBack.visibility = View.INVISIBLE
        } else {
            binding.monthBack.visibility = View.VISIBLE
        }
        if (yearMonth >= maxYearMonth) {
            binding.monthForward.visibility = View.INVISIBLE
        } else {
            binding.monthForward.visibility = View.VISIBLE
        }

        currentYearMonth = yearMonth
    }

    private fun clickOnDate(date: LocalDate) {
        if (rangeDays.contains(date) && isDateInAvailableRanges(date, availableRanges)) {
            selectDate(date)
            (parentFragment as CCTVDetailFragment).archiveCallback(date)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startDate = arguments?.getSerializable(key_startDate) as LocalDate
        endDate = arguments?.getSerializable(key_endDate) as LocalDate
        val ranges = arguments?.getSerializable(key_availableRanges) as Array<*>
        availableRanges.clear()
        ranges.forEach {
            availableRanges.add(it as AvailableRange)
        }
        rangeDays = startDate.rangeTo(endDate)
        val daysOfWeek = daysOfWeekFromLocale()
        minYearMonth = startDate.yearMonth
        maxYearMonth = endDate.yearMonth
        currentYearMonth = maxYearMonth

        binding.archiveCalendar.setup(
            minYearMonth,
            maxYearMonth,
            daysOfWeek.first()
        )
        binding.monthBack.setOnClickListener {
            scrollToMonth(false)
        }
        binding.monthForward.setOnClickListener {
            scrollToMonth(true)
        }
        binding.archiveCalendar.scrollToMonth(currentYearMonth)

        binding.archiveCalendar.dayBinder = CalendarDayBinder(
            today = endDate,
            rangeDays = rangeDays,
            availableRanges = availableRanges,
            notifyDate = { old, new ->
                old?.let {
                    binding.archiveCalendar.notifyDateChanged(it)
                }
                binding.archiveCalendar.notifyDateChanged(new)
            },
            clickOnDate = this::clickOnDate
        )

        binding.archiveCalendar.monthScrollListener = {
            updateModButtons(it.yearMonth)
            binding.monthTitle.text = if (it.year == endDate.year) {
                titleSameYearFormatter.format(it.yearMonth).capitalize()
            } else {
                titleFormatter.format(it.yearMonth).capitalize()
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = view.findViewById<LinearLayout>(R.id.legendLayout)
        }
        binding.archiveCalendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }
                        .forEachIndexed { _, tv ->
                            // tv.text = daysOfWeek[index].name.first().toString()
                            tv.setTextColorRes(R.color.calendar_black)
                        }
                }
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        (binding.archiveCalendar.dayBinder as CalendarDayBinder).selectDate(date)
    }
}
