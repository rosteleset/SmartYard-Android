package ru.madbrains.smartyard.ui.main.address.workSoon.courier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_work_soon_courier.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.address.models.IssueModel
import ru.madbrains.smartyard.ui.main.address.workSoon.office.WorkSoonOfficeFragmentArgs

class WorkSoonCourierFragment : Fragment() {

    private val viewModel by viewModel<WorkSoonCourierViewModel>()
    private var issueModel: IssueModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_work_soon_courier, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireNotNull(arguments).let {
            issueModel = WorkSoonOfficeFragmentArgs.fromBundle(it).issueModel
        }
        btnOk.setOnClickListener {
            viewModel.changeDelivery(
                "Cменился способ доставки. Подготовить пакет для курьера.",
                issueModel?.key ?: "",
                "Курьер"
            )
        }
        btnCancel.setOnClickListener {
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
