package com.sesameware.smartyard_oem.ui.reg.providers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.afterTextChanged
import com.sesameware.smartyard_oem.databinding.FragmentProvidersBinding

class ProvidersFragment : Fragment() {
    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<ProvidersViewModel>()
    private lateinit var adapter: ProvidersAdapter
    private var providerId = ""
    private var providerBaseUrl = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()

        mViewModel.providersList.observe(
            viewLifecycleOwner
        ) {
            filterProvidersItems()
        }

        binding.etFilterProviders.afterTextChanged {
            filterProvidersItems()
        }

        binding.btnChooseProvider.setOnClickListener {
            mViewModel.goToNext(this, providerId, providerBaseUrl)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        activity?.window?.setSoftInputMode(
            if (hidden)
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            else
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
        super.onHiddenChanged(hidden)
    }

    override fun onStop() {
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        super.onStop()
    }

    private fun initRecycler() {
        activity?.let {
            binding.rvProviders.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ProvidersAdapter(it) { id, baseUrl ->
                providerId = id
                providerBaseUrl = baseUrl
                binding.btnChooseProvider.isEnabled = providerId.isNotEmpty() && providerBaseUrl.isNotEmpty()
            }
            binding.rvProviders.adapter = adapter
            binding.rvProviders.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun filterProvidersItems() {
        mViewModel.providersList.value?.let {
            adapter.items = it.map { item ->
                ProviderModel(item.id, item.name, item.baseUrl, false)
            }.filter { item ->
                binding.etFilterProviders.text.isEmpty() || item.name.contains(binding.etFilterProviders.text, true)
            }
            adapter.notifyDataSetChanged()
            binding.btnChooseProvider.isEnabled = false
            providerId = ""
            providerBaseUrl = ""
        }
    }
}
