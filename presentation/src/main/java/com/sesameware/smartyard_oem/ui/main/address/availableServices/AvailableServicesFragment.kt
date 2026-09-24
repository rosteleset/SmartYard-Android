package com.sesameware.smartyard_oem.ui.main.address.availableServices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.TF
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentAvailableServicesBinding
import com.sesameware.smartyard_oem.ui.DividerItemDecorator
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToMargin
import com.sesameware.smartyard_oem.ui.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.injectOrNull

class AvailableServicesFragment : Fragment() {
    private var _binding: FragmentAvailableServicesBinding? = null
    private val binding get() = _binding!!
    private val delegate: AvailableServicesDelegate?
        by injectOrNull(AvailableServicesDelegate::class.java)

    private val viewModel by viewModel<AvailableServicesViewModel>()
    private var servicesList = mutableListOf<AvailableModel>()
    private lateinit var adapter: AvailableAdapter
    private var address = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailableServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.imageView7.setOnClickListener {
            this.findNavController().popBackStack()
        }
        initRecycler()
        arguments?.let { it ->
            servicesList = AvailableServicesFragmentArgs.fromBundle(it).servicesList.map { item ->
                AvailableModel(
                    item.byDefault == TF.TRUE.value,
                    item.byDefault == TF.TRUE.value,
                    item.title,
                    item.description
                )
            }.sortedByDescending { it.isMandatory }.toMutableList()
            adapter.items = servicesList
            address = AvailableServicesFragmentArgs.fromBundle(it).address
        }
        binding.tvAddress.text = address
        binding.btnNext.applyBottomNavInsetsToMargin()
        binding.btnNext.setOnClickListener {
            viewModel.checkServices(servicesList, address)
        }
        setupObserve()
        viewModel.availableOkBtn(servicesList)

        delegate?.extendConfig(
            binding,
            viewModel,
            viewLifecycleOwner,
            findNavController(),
            servicesList,
            address
        )
    }

    private fun setupObserve() {
        viewModel.stateEnabledButtonNext.observe(
            viewLifecycleOwner,
            EventObserver { enabled ->
                binding.btnNext.isEnabled = enabled
            }
        )
        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                (activity as MainActivity?)?.reloadToAddress()
            }
        )
        viewModel.navigateToAddressVerificationFragmentAction.observe(
            viewLifecycleOwner,
            EventObserver {
                val action =
                    AvailableServicesFragmentDirections.actionAvailableServicesFragmentToAddressVerificationFragment(
                        address
                    )
                this.findNavController().navigate(action)
            }
        )
    }

    private fun initRecycler() {
        val dividerItemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.divider
                )
            )
        binding.rvAvailable.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(dividerItemDecoration)
        }
        adapter = AvailableAdapter {
            viewModel.availableOkBtn(servicesList)
        }
        val list = mutableListOf<AvailableModel>()
        list.add(
            AvailableModel(
                isMandatory = true,
                isChecked = true,
                title = "Умный домофон",
                description = "На шлагбаум, ворота и подъезд"
            )
        )
        list.add(
            AvailableModel(
                isMandatory = true,
                isChecked = true,
                title = "Видеонаблюдение",
                description = "3 камеры"
            )
        )
        list.add(
            AvailableModel(
                isMandatory = false,
                isChecked = false,
                title = "Интернет и ТВ",
                description = "Больше 250 каналов"
            )
        )
        adapter.items = list
        binding.rvAvailable.adapter = adapter
    }
}
