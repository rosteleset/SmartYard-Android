package com.sesameware.smartyard_oem.ui.main.address.event_log

import android.annotation.SuppressLint
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
import com.sesameware.data.DataModule
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.threeten.bp.LocalDate
import com.sesameware.domain.model.response.Plog
import com.sesameware.lib.dpToPx
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentEventLogBinding
import com.sesameware.smartyard_oem.ui.DatePickerFragment
import com.sesameware.smartyard_oem.ui.main.address.event_log.adapters.EventLogParentAdapter
import timber.log.Timber

class EventLogFragment : Fragment() {
    private var _binding: FragmentEventLogBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by sharedViewModel<EventLogViewModel>()

    private var needScrollToPosition = -1
    private var prevLoadedIndex = -1
    private var savedLogTypeWidth = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentEventLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)

        binding.spinnerEventLogType.adapter = ArrayAdapter.createFromResource(requireContext(),
            R.array.event_log_types, R.layout.item_event_log_type_spinner).apply {
            setDropDownViewResource(R.layout.event_log_type_spinner_drop_down)
        }

        binding.spinnerEventLogType.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long) {

                //изменение ширины фильтра "Тип события" в зависимости от выбранного
                (parent?.getChildAt(0) as? TextView)?.let {
                    val lp = binding.spinnerEventLogType.layoutParams
                    it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    savedLogTypeWidth = it.measuredWidth + 50.dpToPx()
                    lp.width = savedLogTypeWidth
                    binding.spinnerEventLogType.layoutParams = lp
                    binding.spinnerEventLogType.requestLayout()
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

                mViewModel.loadEventDaysFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        if (mViewModel.flatsAll.size <= 1) {
            binding.spinnerEventLogFlats.isVisible = false
        } else {
            binding.spinnerEventLogFlats.isVisible = true
            binding.spinnerEventLogFlats.adapter = ArrayAdapter(requireContext(),
                R.layout.item_event_log_flats_spinner,
                mViewModel.flatsAll.map {getString(R.string.event_log_flat, it.flatNumber)}.toMutableList().also {
                    it.add(0, getString(R.string.event_log_all_flats))
                }).apply {
                setDropDownViewResource(R.layout.event_log_flats_spinner_drop_down)
            }
            binding.spinnerEventLogFlats.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
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

                    mViewModel.loadEventDaysFilter()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        binding.ivEventLogCalendar.setOnClickListener {
            val llm = binding.rvEventLogParent.layoutManager as LinearLayoutManager
            val eventsDay = (binding.rvEventLogParent.adapter as EventLogParentAdapter).eventsDay
            val p = llm.findFirstVisibleItemPosition()
            val ld = if (p == RecyclerView.NO_POSITION) LocalDate.now() else eventsDay[p]
            val minDate = LocalDate.now().minusMonths(EventLogViewModel.EVENT_LOG_KEEPING_MONTHS)
            val dateDialogFragment = DatePickerFragment(ld, DataModule.serverTz, minDate) {
                scrollToDay(it)
            }
            dateDialogFragment.show(requireActivity().supportFragmentManager, "datePicker")
        }

        binding.srlEventLog.setOnRefreshListener {
            binding.srlEventLog.isRefreshing = false
            prevLoadedIndex = -1
            mViewModel.currentEventItem = null

            mViewModel.loadEventDaysFilter()
        }

        initRecycler()
        binding.ivEventLogBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.fabEventLogScrollUp.setOnClickListener {
            smoothScrollTo(binding.rvEventLogParent.layoutManager, 0)
        }
    }

    override fun onResume() {
        super.onResume()

        //изменение ширины фильтра "Тип события" в зависимости от выбранного
        if (savedLogTypeWidth > 0) {
            val lp = binding.spinnerEventLogType.layoutParams
            lp.width = savedLogTypeWidth
            binding.spinnerEventLogType.layoutParams = lp
            binding.spinnerEventLogType.requestLayout()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecycler() {
        binding.rvEventLogParent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = EventLogParentAdapter(listOf(), hashMapOf()) {
                mViewModel.currentEventItem = it
                this.findNavController().navigate(R.id.action_eventLogFragment_to_eventLogDetailFragment)
            }
        }

        binding.rvEventLogParent.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dx != 0 || dy != 0) {
                    mViewModel.currentEventDayFilter = null
                }

                val llm = binding.rvEventLogParent.layoutManager as LinearLayoutManager
                val itemCount = binding.rvEventLogParent.adapter?.itemCount ?: 0
                if (dy > 0 && llm.findLastVisibleItemPosition() == itemCount - 1
                    && itemCount < mViewModel.eventDaysFilter.size) {
                    mViewModel.loadMoreEvents()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.fabEventLogScrollUp.isVisible = recyclerView.canScrollVertically(-1)
                }
            }
        })

        mViewModel.lastLoadedDayFilterIndex.observe(viewLifecycleOwner) { lastLoadedIndex ->
            if (prevLoadedIndex >= lastLoadedIndex) {
                prevLoadedIndex = lastLoadedIndex
                if (lastLoadedIndex < 0) {
                    (binding.rvEventLogParent.adapter as EventLogParentAdapter).also { adapter ->
                        adapter.eventsDay = listOf()
                        adapter.eventsByDays = hashMapOf()
                        adapter.notifyDataSetChanged()
                    }
                }
                return@observe
            }

            (binding.rvEventLogParent.adapter as EventLogParentAdapter).also { adapter ->
                adapter.eventsDay =
                    mViewModel.eventDaysFilter.map { it.day }.subList(0, lastLoadedIndex + 1)
                adapter.eventsByDays = mViewModel.eventsByDaysFilter
                if (prevLoadedIndex < 0) {
                    adapter.notifyDataSetChanged()
                    mViewModel.currentEventDayFilter?.let {
                        val p = mViewModel.getEventDayFilterIndex(it)
                        if (p >= 0) {
                            smoothScrollTo(binding.rvEventLogParent.layoutManager, p)
                        }
                    }
                } else {
                    adapter.notifyItemRangeInserted(
                        prevLoadedIndex + 1,
                        lastLoadedIndex - prevLoadedIndex
                    )
                }
                if (needScrollToPosition >= 0) {
                    smoothScrollTo(binding.rvEventLogParent.layoutManager, needScrollToPosition)
                    needScrollToPosition = -1
                }
            }

            prevLoadedIndex = lastLoadedIndex
        }

        mViewModel.progress.observe(viewLifecycleOwner) {
            binding.pbEventLog.isVisible = it
        }

    }

    private fun scrollToDay(day: LocalDate) {
        val llm = binding.rvEventLogParent.layoutManager as LinearLayoutManager
        val eventsDay = (binding.rvEventLogParent.adapter as EventLogParentAdapter).eventsDay

        Timber.d("__Q__ eventsDay: $eventsDay")

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
                    Timber.d("__Q__ mViewModel.eventDaysFilter: ${mViewModel.eventDaysFilter}")
                    for (i in eventsDay.size until mViewModel.eventDaysFilter.size) {
                        days.add(mViewModel.eventDaysFilter[i].day)
                        if (day >= mViewModel.eventDaysFilter[i].day) {
                            val day0 = day.toEpochDay()
                            val day1 = mViewModel.eventDaysFilter[i - 1].day.toEpochDay()
                            val day2 = mViewModel.eventDaysFilter[i].day.toEpochDay()
                            nearestIndex = if (day1 - day0 <= day0 - day2) i - 1 else i
                            break
                        }
                    }
                    if (nearestIndex < 0) {
                        nearestIndex = mViewModel.eventDaysFilter.size - 1
                    }
                    if (days.isNotEmpty()) {
                        if (nearestIndex >= 0) {
                            needScrollToPosition = nearestIndex
                        }
                        Timber.d("__Q__ event days: $days")
                        mViewModel.loadMoreEvents(days)
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
