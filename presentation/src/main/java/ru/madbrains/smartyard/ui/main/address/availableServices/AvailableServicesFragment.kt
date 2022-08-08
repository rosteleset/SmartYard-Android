package ru.madbrains.smartyard.ui.main.address.availableServices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.TF
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAvailableServicesBinding
import ru.madbrains.smartyard.ui.DividerItemDecorator
import ru.madbrains.smartyard.ui.main.MainActivity

class AvailableServicesFragment : Fragment() {
    private var _binding: FragmentAvailableServicesBinding? = null
    private val binding get() = _binding!!

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

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.imageView7.setOnClickListener {
            this.findNavController().popBackStack()
        }
        initRecycler()
        arguments?.let {
            servicesList = AvailableServicesFragmentArgs.fromBundle(it).servicesList.map {
                AvailableModel(
                    it.canChange == TF.TRUE.value,
                    it.byDefault == TF.TRUE.value,
                    it.title,
                    it.description
                )
            }.sortedBy { it.active }.toMutableList()
            adapter.items = servicesList
            address = AvailableServicesFragmentArgs.fromBundle(it).address
        }
        binding.tvAddress.text = address
        binding.btnNext.setOnClickListener {
            viewModel.checkServices(servicesList, address)
        }
        setupObserve()
        viewModel.avalaibleOkayBtn(servicesList)
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
            viewModel.avalaibleOkayBtn(servicesList)
        }
        val list = mutableListOf<AvailableModel>()
        list.add(
            AvailableModel(
                active = false,
                check = true,
                title = "Умный домофон",
                description = "На шлагбаум, ворота и подъезд"
            )
        )
        list.add(
            AvailableModel(
                active = false,
                check = true,
                title = "Видеонаблюдение",
                description = "3 камеры"
            )
        )
        list.add(
            AvailableModel(
                active = true,
                check = false,
                title = "Интернет и ТВ",
                description = "Больше 250 каналов"
            )
        )
        adapter.items = list
        binding.rvAvailable.adapter = adapter
    }
}
