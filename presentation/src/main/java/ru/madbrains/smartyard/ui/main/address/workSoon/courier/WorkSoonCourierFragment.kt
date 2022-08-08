package ru.madbrains.smartyard.ui.main.address.workSoon.courier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.databinding.FragmentWorkSoonCourierBinding
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.models.IssueModel
import ru.madbrains.smartyard.ui.main.address.workSoon.office.WorkSoonOfficeFragmentArgs

class WorkSoonCourierFragment : Fragment() {
    private var _binding: FragmentWorkSoonCourierBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<WorkSoonCourierViewModel>()
    private var issueModel: IssueModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkSoonCourierBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireNotNull(arguments).let {
            issueModel = WorkSoonOfficeFragmentArgs.fromBundle(it).issueModel
        }
        binding.btnOk.setOnClickListener {
            viewModel.changeDelivery(
                "Cменился способ доставки. Подготовить пакет для курьера.",
                issueModel?.key ?: "",
                "Курьер"
            )
        }
        binding.btnCancel.setOnClickListener {
            viewModel.deleteIssue(issueModel?.key ?: "")
        }
        setupObserve()
    }

    private fun setupObserve() {
        viewModel.successNavigateToFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                (activity as MainActivity?)?.reloadToAddress()
            }
        )
        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                (activity as MainActivity?)?.reloadToAddress()
            }
        )
    }
}
