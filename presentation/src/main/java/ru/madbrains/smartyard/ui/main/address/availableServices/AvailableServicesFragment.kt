package ru.madbrains.smartyard.ui.main.address.availableServices

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.response.ServicesData
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAvailableServicesBinding
import ru.madbrains.smartyard.ui.DividerItemDecorator
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.addressVerification.AddressVerificationFragment
import timber.log.Timber

class AvailableServicesFragment : Fragment() {
    private var _binding: FragmentAvailableServicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<AvailableServicesViewModel>()
    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.imageView7.setOnClickListener {
//            this.findNavController().popBackStack()
            parentFragmentManager.popBackStack()
        }
        initRecycler()
        arguments?.let {
//            servicesList = AvailableServicesFragmentArgs.fromBundle(it).servicesList.map {
//                AvailableModel(
//                    it.canChange == TF.TRUE.value,
//                    it.byDefault == TF.TRUE.value,
//                    it.title,
//                    it.description
//                )
//            }.sortedBy { it.active }.toMutableList()
            val serviceData = it.getParcelableArray("servicesList")?.mapNotNull { sd -> sd as? ServicesData}
            servicesList = serviceData?.map { it1 ->
                AvailableModel(
//                    it1.canChange == TF.TRUE.value,
//                    it1.byDefault == TF.TRUE.value,
                    true,
                    true,
                    it1.title,
                    it1.description
                )
            }?.sortedBy { it.active }?.toMutableList()?: mutableListOf(AvailableModel(false,false,"", ""))

            adapter.items = servicesList
//            address = AvailableServicesFragmentArgs.fromBundle(it).address
            address = it.getString("address").toString()
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
                val countFragment = parentFragmentManager.backStackEntryCount
                for (i in 0 until countFragment){
                    parentFragmentManager.popBackStack()
                }
                mainActivityViewModel.bottomNavigateToIntercom()
                val intentBroadcast = Intent(AddressWebViewFragment.REFRESH_INTENT)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentBroadcast)
                (activity as MainActivity?)?.reloadToAddress()
            }
        )
        viewModel.navigateToAddressVerificationFragmentAction.observe(
            viewLifecycleOwner,
            EventObserver {
//                val action =
//                    AvailableServicesFragmentDirections.actionAvailableServicesFragmentToAddressVerificationFragment(
//                        address
//                    )
//                this.findNavController().navigate(action)
                val transaction = parentFragmentManager.beginTransaction()
                val newFragment = AddressVerificationFragment()
                val bundle = Bundle()
                bundle.putString("address", address)
                newFragment.arguments = bundle
                transaction.add(R.id.cl_avalible_services, newFragment)
                transaction.addToBackStack("AvailableServicesFragment")
                transaction.commit()
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
