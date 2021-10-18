package ru.madbrains.smartyard.ui.main.address.workSoon.office

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_work_soon_office.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.string
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.models.IssueModel

class WorkSoonOfficeFragment() : Fragment() {

    private val viewModel by viewModel<WorkSoonOfficeViewModel>()

    private var issueModel: IssueModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_work_soon_office, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireNotNull(arguments).let {
            issueModel = WorkSoonOfficeFragmentArgs.fromBundle(it).issueModel
        }
        bntQrCode.setOnClickListener {
            this.findNavController().navigate(R.id.action_workSoonOfficeFragment_to_qrCodeFragment)
        }
        tvCaption.text = String.format(
            getString(string.address_work_soon_office_caption), issueModel?.address
        )

        btnCancel.setOnClickListener {
            viewModel.deleteIssue(issueModel?.key ?: "")
        }
        btnOk.setOnClickListener {
            viewModel.changeDelivery(
                "Cменился способ доставки. Клиент подойдет в офис.",
                issueModel?.key ?: "",
                "Самовывоз"
            )
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
