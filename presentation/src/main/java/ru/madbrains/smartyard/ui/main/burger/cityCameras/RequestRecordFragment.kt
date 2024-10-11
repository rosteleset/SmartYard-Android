package ru.madbrains.smartyard.ui.main.burger.cityCameras

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentRequestRecordBinding
import ru.madbrains.smartyard.ui.DatePickerFragment
import ru.madbrains.smartyard.ui.TimePickerFragment
import ru.madbrains.smartyard.ui.showStandardAlert

class RequestRecordFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private var _binding: FragmentRequestRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CityCamerasViewModel by sharedStateViewModel()
    private var recordDate: LocalDate = LocalDate.now()
    private var recordTime: LocalTime = LocalTime.now()
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
            val minDate = LocalDate.now().minusDays(CityCamerasViewModel.RECORD_DEPTH_DAYS)
            val dateDialogFragment = DatePickerFragment(recordDate, minDate) {
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
                   for (i in 0 until  parentFragmentManager.backStackEntryCount){
                       parentFragmentManager.popBackStack()
                   }
                    this.findNavController().popBackStack()
                }
            }
        )
    }

    private fun updateDateTime() {
        var recordDateTime = LocalDateTime.of(recordDate, recordTime)
        if (recordDateTime.plusMinutes(durationList[selectedDurationPosition].toLong()) > LocalDateTime.now()) {
            recordDateTime = LocalDateTime.now().minusMinutes(durationList[selectedDurationPosition].toLong())
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
