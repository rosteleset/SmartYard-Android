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
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentCctvDetailArchiveBinding
import ru.madbrains.smartyard.daysOfWeekFromLocale
import ru.madbrains.smartyard.setTextColorRes
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.detail.CalendarDayBinder
import ru.madbrains.smartyard.ui.main.address.cctv_video.isDateInAvailableRanges
import timber.log.Timber
import java.util.*

class CCTVArchiveTab : Fragment() {
    private var _binding: FragmentCctvDetailArchiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var rangeDays: ClosedRange<LocalDate>
    private var currentYearMonth = YearMonth.now()
    private var minYearMonth = currentYearMonth
    private var maxYearMonth = currentYearMonth

    private val titleSameYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val titleFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    companion object {
        fun newInstance(
        ) = CCTVArchiveTab().apply {
            Timber.d("debug_dmm __new instance $this")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm __onViewCreated")
        mCCTVViewModel.closedRangeCalendar.observe(
            viewLifecycleOwner,
            EventObserver {
                mCCTVViewModel.startDate = it.start
                mCCTVViewModel.endDate = it.endInclusive
                refreshArchiveCalendar()
            }
        )
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
        if (rangeDays.contains(date) && isDateInAvailableRanges(date, mCCTVViewModel.availableRanges)) {
            selectDate(date)
            (parentFragment as CCTVDetailFragment).archiveCallback(date)
        }
    }

    private fun refreshArchiveCalendar() {
        rangeDays = mCCTVViewModel.startDate.rangeTo(mCCTVViewModel.endDate)
        val daysOfWeek = daysOfWeekFromLocale()
        minYearMonth = mCCTVViewModel.startDate.yearMonth
        maxYearMonth = mCCTVViewModel.endDate.yearMonth
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
            today = mCCTVViewModel.endDate,
            rangeDays = rangeDays,
            availableRanges = mCCTVViewModel.availableRanges,
            notifyDate = { old, new ->
                old?.let {
                    binding.archiveCalendar.notifyDateChanged(it)
                }
                binding.archiveCalendar.notifyDateChanged(new)
            },
            clickOnDate = this::clickOnDate
        )

        binding.archiveCalendar.monthScrollListener = { calendarMonth ->
            updateModButtons(calendarMonth.yearMonth)
            binding.monthTitle.text = if (calendarMonth.year == mCCTVViewModel.endDate.year) {
                titleSameYearFormatter.format(calendarMonth.yearMonth)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            } else {
                titleFormatter.format(calendarMonth.yearMonth)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
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

    override fun onResume() {
        super.onResume()

        refreshArchiveCalendar()
    }
}
