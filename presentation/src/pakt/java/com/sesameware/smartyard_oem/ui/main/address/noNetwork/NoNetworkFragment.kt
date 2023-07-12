package com.sesameware.smartyard_oem.ui.main.address.noNetwork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentNoNetworkBinding
import com.sesameware.smartyard_oem.ui.DividerItemDecorator
import com.sesameware.smartyard_oem.ui.main.MainActivity

class NoNetworkFragment : Fragment() {
    private var _binding: FragmentNoNetworkBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<NoNetworkViewModel>()
    private var address = ""
    private val servicesList = mutableListOf(
        ItemService(name = "Умный домофон", check = false),
        ItemService(name = "Видеонаблюдение", check = false),
        ItemService(name = "Интернет", check = false),
        ItemService("Телевидение", false),
        ItemService("Телефония", false)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            address = NoNetworkFragmentArgs.fromBundle(it).address
        }
        initRecycler()
        setupUi()
        setupObserve()
    }

    private fun setupObserve() {
        viewModel.navigateToIssueFragmentAction.observe(
            viewLifecycleOwner,
            EventObserver {
                val action = NoNetworkFragmentDirections.actionNoNetworkFragmentToIssueFragment(it)
                this.findNavController().navigate(action)
            }
        )
    }

    private fun setupUi() {
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.btnCreateIssue.setOnClickListener {
            viewModel.createIssue(address, servicesList.filter { it.check }.map { it.name })
        }
    }

    private fun initRecycler() {
        val dividerItemDecoration: ItemDecoration =
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.divider
                )
            )
        binding.rvServices.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(dividerItemDecoration)
        }
        val adapter = ListDelegationAdapter<List<ItemService>>(
            ServicesAdapterDelegate {
                availableButton(servicesList)
            }
        )
        adapter.items = servicesList
        binding.rvServices.adapter = adapter
    }

    private fun availableButton(list: MutableList<ItemService>) {
        binding.btnCreateIssue.isEnabled = list.any { it.check }
    }

    data class ItemService(
        val name: String,
        var check: Boolean
    )
}
