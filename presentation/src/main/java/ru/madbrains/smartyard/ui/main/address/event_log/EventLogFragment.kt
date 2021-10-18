package ru.madbrains.smartyard.ui.main.address.event_log

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_event_log.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.threeten.bp.LocalDate
import ru.madbrains.domain.model.response.Plog
import ru.madbrains.lib.dpToPx
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.event_log.adapters.EventLogParentAdapter
import ru.madbrains.smartyard.ui.main.burger.cityCameras.DatePickerFragment

class EventLogFragment : Fragment() {
    private val mViewModel by sharedViewModel<EventLogViewModel>()

    private var needScrollToPosition = -1
    private var prevLoadedIndex = -1
    private var savedLogTypeWidth = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_log, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        spinnerEventLogType.adapter = ArrayAdapter.createFromResource(requireContext(),
            R.array.event_log_types, R.layout.item_event_log_type_spinner).apply {
            setDropDownViewResource(R.layout.event_log_type_spinner_drop_down)
        }

        spinnerEventLogType.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long) {

                //изменение ширины фильтра "Тип события" в зависимости от выбранного
                (parent?.getChildAt(0) as? TextView)?.let {
                    val lp = spinnerEventLogType.layoutParams
                    it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    savedLogTypeWidth = it.measuredWidth + 50.dpToPx()
                    lp.width = savedLogTypeWidth
                    spinnerEventLogType.layoutParams = lp
                    spinnerEventLogType.requestLayout()
                }

                val s = mutableSetOf<Int>()
                when (position) {
                    0 -> {
                        s.add(Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED)
                        s.add(Plog.EVENT_DOOR_PHONE_CALL_ANSWERED)
                        s.add(Plog.EVENT_OPEN_BY_KEY)
                        s.add(Plog.EVENT_OPEN_FROM_APP)
                        s.add(Plog.EVENT_OPEN_BY_FACE)
                        s.add(Plog.EVENT_OPEN_BY_CODE)
                        s.add(Plog.EVENT_OPEN_GATES_BY_CALL)
                    }
                    1 -> {
                        s.add(Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED)
                        s.add(Plog.EVENT_DOOR_PHONE_CALL_ANSWERED)
                    }
                    2 -> {
                        s.add(Plog.EVENT_OPEN_BY_KEY)
                    }
                    3 -> {
                        s.add(Plog.EVENT_OPEN_BY_FACE)
                    }
                    4 -> {
                        s.add(Plog.EVENT_OPEN_FROM_APP)
                    }
                    5 -> {
                        s.add(Plog.EVENT_OPEN_GATES_BY_CALL)
                    }
                    6 -> {
                        s.add(Plog.EVENT_OPEN_BY_CODE)
                    }
                }
                mViewModel.filterEventType = s
                mViewModel.currentEventItem = null
                prevLoadedIndex = -1

                mViewModel.getEventDaysFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        if (mViewModel.flatsAll.size <= 1) {
            spinnerEventLogFlats.isVisible = false
        } else {
            spinnerEventLogFlats.isVisible = true
            spinnerEventLogFlats.adapter = ArrayAdapter(requireContext(),
                R.layout.item_event_log_flats_spinner,
                mViewModel.flatsAll.map {"Квартира ${it.flatNumber}"}.toMutableList().also {
                    it.add(0, "Все квартиры")
                }).apply {
                setDropDownViewResource(R.layout.event_log_flats_spinner_drop_down)
            }
            spinnerEventLogFlats.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long) {

                    var flat: Flat? = null
                    if (position > 0) {
                        flat = mViewModel.flatsAll[position - 1]
                    }
                    mViewModel.filterFlat = flat
                    mViewModel.currentEventItem = null
                    prevLoadedIndex = -1

                    mViewModel.getEventDaysFilter()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        ivEventLogCalendar.setOnClickListener {
            val llm = rvEventLogParent.layoutManager as LinearLayoutManager
            val eventsDay = (rvEventLogParent.adapter as EventLogParentAdapter).eventsDay
            val p = llm.findFirstVisibleItemPosition()
            val ld = if (p == RecyclerView.NO_POSITION) LocalDate.now() else eventsDay[p]
            val dateDialogFragment = DatePickerFragment(ld) {
                scrollToDay(it)
            }
            dateDialogFragment.show(requireActivity().supportFragmentManager, "datePicker")
        }

        srlEventLog.setOnRefreshListener {
            srlEventLog.isRefreshing = false
            prevLoadedIndex = -1
            mViewModel.currentEventItem = null

            mViewModel.getEventDaysFilter()
        }

        initRecycler()
        ivEventLogBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        fabEventLogScrollUp.setOnClickListener {
            smoothScrollTo(rvEventLogParent.layoutManager, 0)
        }
    }

    override fun onResume() {
        super.onResume()

        //изменение ширины фильтра "Тип события" в зависимости от выбранного
        if (savedLogTypeWidth > 0) {
            val lp = spinnerEventLogType.layoutParams
            lp.width = savedLogTypeWidth
            spinnerEventLogType.layoutParams = lp
            spinnerEventLogType.requestLayout()
        }
    }

    private fun initRecycler() {
        rvEventLogParent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = EventLogParentAdapter(listOf(), hashMapOf()) {
                mViewModel.currentEventItem = it
                this.findNavController().navigate(R.id.action_eventLogFragment_to_eventLogDetailFragment)
            }
        }

        rvEventLogParent.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dx != 0 || dy != 0) {
                    mViewModel.currentEventDayFilter = null
                }

                val llm = rvEventLogParent.layoutManager as LinearLayoutManager
                val itemCount = rvEventLogParent.adapter?.itemCount ?: 0
                if (dy > 0 && llm.findLastVisibleItemPosition() == itemCount - 1
                    && itemCount < mViewModel.eventDaysFilter.size) {
                    mViewModel.getMoreEvents()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fabEventLogScrollUp.isVisible = recyclerView.canScrollVertically(-1)
                }
            }
        })

        mViewModel.lastLoadedDayFilterIndex.observe(viewLifecycleOwner, { lastLoadedIndex ->
                if (prevLoadedIndex >= lastLoadedIndex) {
                    prevLoadedIndex = lastLoadedIndex
                    if (lastLoadedIndex < 0) {
                        (rvEventLogParent.adapter as EventLogParentAdapter).also {adapter ->
                            adapter.eventsDay = listOf()
                            adapter.eventsByDays = hashMapOf()
                            adapter.notifyDataSetChanged()
                        }
                    }
                    return@observe
                }

                (rvEventLogParent.adapter as EventLogParentAdapter).also { adapter ->
                    adapter.eventsDay = mViewModel.eventDaysFilter.map {it.day}.subList(0, lastLoadedIndex + 1)
                    adapter.eventsByDays = mViewModel.eventsByDaysFilter
                    if (prevLoadedIndex < 0) {
                        adapter.notifyDataSetChanged()
                        mViewModel.currentEventDayFilter?.let {
                            val p = mViewModel.getEventDayFilterIndex(it)
                            if (p >= 0) {
                                smoothScrollTo(rvEventLogParent.layoutManager, p)
                            }
                        }
                    } else {
                        adapter.notifyItemRangeInserted(prevLoadedIndex + 1,
                            lastLoadedIndex - prevLoadedIndex)
                    }
                    if (needScrollToPosition >= 0) {
                        smoothScrollTo(rvEventLogParent.layoutManager, needScrollToPosition)
                        needScrollToPosition = -1
                    }
                }

                prevLoadedIndex = lastLoadedIndex
            })

        mViewModel.progress.observe(viewLifecycleOwner, {
            pbEventLog.isVisible = it
        })
        
    }

    private fun scrollToDay(day: LocalDate) {
        val llm = rvEventLogParent.layoutManager as LinearLayoutManager
        val eventsDay = (rvEventLogParent.adapter as EventLogParentAdapter).eventsDay
        var nearestIndex = -1
        if (eventsDay.isNotEmpty())
        {
            if (eventsDay.first() >= day && day >= eventsDay.last()) {
                for (i in 0 until eventsDay.size - 1) {
                    if (eventsDay[i] >= day && day >= eventsDay[i + 1]) {
                        val day0 = day.toEpochDay()
                        val day1 = eventsDay[i].toEpochDay()
                        val day2 = eventsDay[i + 1].toEpochDay()
                        nearestIndex = if (day1 - day0 <= day0 - day2 ) i else i + 1
                        break
                    }
                }
                if (nearestIndex >= 0) {
                    smoothScrollTo(llm, nearestIndex)
                }

            } else {
                if (day > eventsDay.first()) {
                    smoothScrollTo(llm, 0)
                } else {
                    val days = mutableListOf<LocalDate>()
                    for (i in eventsDay.size until mViewModel.eventDaysFilter.size) {
                        days.add(mViewModel.eventDaysFilter[i].day)
                        if (day >= mViewModel.eventDaysFilter[i].day) {
                            val day0 = day.toEpochDay()
                            val day1 = mViewModel.eventDaysFilter[i - 1].day.toEpochDay()
                            val day2 = mViewModel.eventDaysFilter[i].day.toEpochDay()
                            nearestIndex = if (day1 - day0 <= day0 - day2) i - 1 else i
                        }
                    }
                    if (nearestIndex < 0) {
                        nearestIndex = mViewModel.eventDaysFilter.size - 1
                    }
                    if (days.isNotEmpty()) {
                        if (nearestIndex >= 0) {
                            needScrollToPosition = nearestIndex
                        }
                        mViewModel.getMoreEvents(days)
                    }
                }
            }
        }
    }

    private fun smoothScrollTo(layoutManager: RecyclerView.LayoutManager?, position: Int) {
        val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }
}
