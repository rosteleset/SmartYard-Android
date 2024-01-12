package com.sesameware.smartyard_oem.ui.main.burger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentBurgerBinding
import com.sesameware.smartyard_oem.ui.showStandardAlert

class BurgerFragment : Fragment() {
    private var _binding: FragmentBurgerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BurgerViewModel by sharedViewModel()

    private lateinit var adapter: ListDelegationAdapter<List<BurgerModel>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentBurgerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llCallSupport.setOnClickListener {
            viewModel.getHelpMe()
            val dialog = CallToSupportFragment()
            dialog.show(requireActivity().supportFragmentManager, "callToSupport")
        }

        initRecycler()
        setupObservers()
    }

    private fun initRecycler() {
        binding.rvBurger.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter(
            BurgerDelegate()
        )
        binding.rvBurger.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.chosenSupportOption.observe(
            viewLifecycleOwner
        ) {
            it?.let { supportOption ->
                when (supportOption) {
                    BurgerViewModel.SupportOption.CALL_TO_SUPPORT_BY_PHONE -> {
                        viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.NONE)
                        callToSupportByPhone(
                            viewModel.dialNumber.value ?: ""
                        )
                    }
                    BurgerViewModel.SupportOption.ORDER_CALLBACK -> {
                        viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.NONE)
                        orderCallback()
                    }
                    else -> {}
                }
            }
        }

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(requireContext(), R.string.issue_dialog_caption_0)
            }
        )

        viewModel.burgerList.observe(
            viewLifecycleOwner
        ) {
            adapter.items = it
            adapter.notifyDataSetChanged()
        }

        viewModel.navigateToFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                this.findNavController().navigate(it)
            }
        )

        viewModel.navigateToWebView.observe(
            viewLifecycleOwner,
            EventObserver{
                val action = BurgerFragmentDirections.actionBurgerFragmentToCustomWebViewFragmentSettings(
                    R.id.customWebViewFragmentSettings,
                    R.id.customWebBottomFragmentSettings,
                    it.basePath,
                    it.code,
                    resources.getString(R.string.title_burger))
                this.findNavController().navigate(action)
            }
        )
    }

    private fun callToSupportByPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun orderCallback() {
        viewModel.createIssue()
    }
}