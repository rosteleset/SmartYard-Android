package com.sesameware.smartyard_oem.ui.main.address.issue

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.databinding.FragmentIssueBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.availableServices.AvailableServicesViewModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class IssueFragment : Fragment() {
    private var _binding: FragmentIssueBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<AvailableServicesViewModel>()
    private var issueModel: IssueModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireNotNull(arguments).let {
            issueModel = IssueFragmentArgs.fromBundle(it).issueModel
        }

        binding.btnCancel.setOnClickListener {
            viewModel.deleteIssue(issueModel?.key ?: "")
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.successNavigateToFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                (activity as? MainActivity)?.reloadToAddress()
            }
        )
    }
}
