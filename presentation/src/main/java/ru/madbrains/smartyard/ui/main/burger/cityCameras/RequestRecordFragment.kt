package ru.madbrains.smartyard.ui.main.burger.cityCameras

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_request_record.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.madbrains.smartyard.utils.stateSharedViewModel

class DatePickerFragment(
    private val recordDate: LocalDate,
    private val callback: listenerGeneric<LocalDate>) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return DatePickerDialog(requireContext(), this,
            recordDate.year, recordDate.monthValue - 1, recordDate.dayOfMonth)
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        callback(LocalDate.of(year, month + 1, dayOfMonth))
    }
}

class TimePickerFragment(
    private val recordTime: LocalTime,
    private val callback: listenerGeneric<LocalTime>) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(requireContext(), this, recordTime.hour, recordTime.minute,
            DateFormat.is24HourFormat(requireContext()))
    }
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        callback(LocalTime.of(hourOfDay, minute))
    }
}


class RequestRecordFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private val viewModel: CityCamerasViewModel by stateSharedViewModel()
    private var recordDate: LocalDate = LocalDate.now()
    private var recordTime: LocalTime = LocalTime.NOON
    private val durationList = listOf(5, 10, 15, 20, 30, 40, 50, 60)  // список возможных вариантов продолжительности записи в минутах
    private var selectedDurationPosition = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_request_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        ivRequestRecordBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        ivRequestRecordDateArrow.setOnClickListener {
            val dateDialogFragment = DatePickerFragment(recordDate) {
                recordDate = it
                updateDate()
            }
            dateDialogFragment.show(requireActivity().supportFragmentManager, "datePicker")
        }

        ivRequestRecordTimeArrow.setOnClickListener {
            val timeDialogFragment = TimePickerFragment(recordTime) {
                recordTime = it
                updateTime()
            }
            timeDialogFragment.show(requireActivity().supportFragmentManager, "timePicker")
        }

        spinnerRequestRecordDuration.onItemSelectedListener = this
        spinnerRequestRecordDuration.setSelection(selectedDurationPosition)

        btnRequestRecord.setOnClickListener {
            viewModel.createIssue(recordDate, recordTime, durationList[selectedDurationPosition], tvRequestRecordComments.text.toString())
        }

        updateDate()
        updateTime()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.chosenCamera.observe(
            viewLifecycleOwner,
            {
                it?.run {
                    val slash = this.name.indexOf("/")
                    tvRequestRecordTitleSub.text = this.name.substring(slash + 1).trim()
                }
            }
        )

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(requireContext(), R.string.issue_dialog_caption_0) {
                    this.findNavController().popBackStack()
                }
            }
        )
    }

    private fun updateDate() {
        tvRequestRecordDate.text = resources.getString(R.string.request_record_date, recordDate.format(
            DateTimeFormatter.ofPattern("d.MM.yyyy")))
    }

    private fun updateTime() {
        tvRequestRecordTime.text = resources.getString(R.string.request_record_time, recordTime.format(
            DateTimeFormatter.ofPattern("HH-mm")))
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedDurationPosition = position
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        
    }
}