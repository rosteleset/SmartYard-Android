package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentRequestRecordBinding
import com.sesameware.smartyard_oem.ui.DatePickerFragment
import com.sesameware.smartyard_oem.ui.TimePickerFragment
import com.sesameware.smartyard_oem.ui.showStandardAlert

class RequestRecordFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private var _binding: FragmentRequestRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityCamerasViewModel by sharedStateViewModel()
    private var recordDate: LocalDate = LocalDate.now(ZoneId.of(DataModule.serverTz))
    private var recordTime: LocalTime = LocalTime.now(ZoneId.of(DataModule.serverTz))
    private val durationList = listOf(5, 10, 15, 20, 30, 40, 50, 60)  // список возможных вариантов продолжительности записи в минутах
    private var selectedDurationPosition = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentRequestRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.ivRequestRecordBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.ivRequestRecordDateArrow.setOnClickListener {
            val minDate = LocalDate.now(ZoneId.of(DataModule.serverTz)).minusDays(CityCamerasViewModel.RECORD_DEPTH_DAYS)
            val dateDialogFragment = DatePickerFragment(recordDate, DataModule.serverTz, minDate) {
                recordDate = it
                updateDateTime()
            }
            dateDialogFragment.show(requireActivity().supportFragmentManager, "datePicker")
        }

        binding.ivRequestRecordTimeArrow.setOnClickListener {
            val timeDialogFragment = TimePickerFragment(recordTime) {
                recordTime = it
                updateDateTime()
            }
            timeDialogFragment.show(requireActivity().supportFragmentManager, "timePicker")
        }

        binding.spinnerRequestRecordDuration.onItemSelectedListener = this
        binding.spinnerRequestRecordDuration.setSelection(selectedDurationPosition)

        binding.btnRequestRecord.setOnClickListener {
            viewModel.createIssue(recordDate, recordTime, durationList[selectedDurationPosition],
                binding.tvRequestRecordComments.text.toString())
        }

        updateDateTime()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) {
            it?.run {
                val slash = this.name.indexOf("/")
                binding.tvRequestRecordTitleSub.text = this.name.substring(slash + 1).trim()
            }
        }

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(requireContext(), R.string.issue_dialog_caption_0) {
                    this.findNavController().popBackStack()
                }
            }
        )
    }

    private fun updateDateTime() {
        var recordDateTime = LocalDateTime.of(recordDate, recordTime)
        if (recordDateTime.plusMinutes(durationList[selectedDurationPosition].toLong()) > LocalDateTime.now(ZoneId.of(DataModule.serverTz))) {
            recordDateTime = LocalDateTime.now(ZoneId.of(DataModule.serverTz)).minusMinutes(durationList[selectedDurationPosition].toLong())
            recordDate = recordDateTime.toLocalDate()
            recordTime = recordDateTime.toLocalTime()
        }
        updateDate()
        updateTime()
    }

    private fun updateDate() {
        binding.tvRequestRecordDate.text = resources.getString(R.string.request_record_date, recordDate.format(
            DateTimeFormatter.ofPattern("d.MM.yyyy")))
    }

    private fun updateTime() {
        binding.tvRequestRecordTime.text = resources.getString(R.string.request_record_time, recordTime.format(
            DateTimeFormatter.ofPattern("HH-mm")))
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedDurationPosition = position
        updateDateTime()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        
    }
}
