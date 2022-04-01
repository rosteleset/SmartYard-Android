package ru.madbrains.smartyard.ui.main.address.inputAdress

import android.R.layout
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.response.HousesData
import ru.madbrains.domain.model.response.LocationData
import ru.madbrains.domain.model.response.StreetsData
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.afterTextChanged
import ru.madbrains.smartyard.databinding.FragmentInputAddressBinding
import ru.madbrains.smartyard.hideKeyboard

class InputAddressFragment : Fragment() {
    private var _binding: FragmentInputAddressBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<InputAddressViewModel>()

    private lateinit var cityAdapter: CityAdapter

    private lateinit var streetAdapter: StreetAdapter

    private lateinit var houseAdapter: HousesAdapter

    private var houseId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initAutoCompleteTextView() {
        cityAdapter = CityAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvCity.setAdapter(cityAdapter)
        binding.actvCity.setOnItemClickListener() { parent, _, position, id ->
            val selectItem = parent.adapter.getItem(position) as LocationData
            binding.actvCity.setText(selectItem.name)
            mViewModel.getStreet(selectItem.locationId)
        }

        streetAdapter =
            StreetAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvStreet.setAdapter(streetAdapter)
        binding.actvStreet.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as StreetsData?
            binding.actvStreet.setText(selectedItem?.name)
            mViewModel.getHouses(selectedItem?.streetId ?: 0)
        }

        houseAdapter =
            HousesAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvHouse.setAdapter(houseAdapter)
        binding.actvHouse.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as HousesData?
            binding.actvHouse.setText(selectedItem?.number)
            houseId = selectedItem?.houseId
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAutoCompleteTextView()

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.btnCheckAvailableServices.setOnClickListener {
            hideKeyboard(requireActivity())
            mViewModel.getServices(houseId = houseId, address = getAddress())
        }

        mViewModel.navigationToNoNetwork.observe(
            viewLifecycleOwner,
            EventObserver {
                val action =
                    InputAddressFragmentDirections.actionInputAddressFragmentToNoNetworkFragment(it)
                this.findNavController().navigate(action)
            }
        )

        mViewModel.cityList.observe(
            viewLifecycleOwner,
            Observer {
                cityAdapter.addData(it)
            }
        )

        mViewModel.progressCity.observe(
            viewLifecycleOwner,
            Observer {
                binding.progressCity.isVisible = it
            }
        )

        mViewModel.progressStreet.observe(
            viewLifecycleOwner,
            Observer {
                binding.progressStreet.isVisible = it
            }
        )

        mViewModel.progressHouse.observe(
            viewLifecycleOwner,
            Observer {
                binding.progressHouse.isVisible = it
            }
        )

        mViewModel.streetList.observe(
            viewLifecycleOwner,
            Observer {
                streetAdapter.addData(it)
            }
        )

        mViewModel.houseList.observe(
            viewLifecycleOwner,
            Observer {
                houseAdapter.addData(it)
            }
        )

        mViewModel.confirmError.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.progressCity.isVisible = false
                binding.progressStreet.isVisible = false
                binding.progressHouse.isVisible = false
            }
        )

        mViewModel.servicesList.observe(
            viewLifecycleOwner,
            EventObserver {
                val address = if (binding.etApartment.text.isNotEmpty())
                    "${binding.actvCity.text}, ${binding.actvStreet.text}, ${binding.actvHouse.text}, ${binding.etApartment.text}" else
                    "${binding.actvCity.text}, ${binding.actvStreet.text}, ${binding.actvHouse.text}"
                val action =
                    InputAddressFragmentDirections.actionInputAddressFragmentToAvailableServicesFragment(
                        it.toTypedArray(),
                        address
                    )
                this.findNavController().navigate(action)
            }
        )
        binding.actvCity.afterTextChanged(this::validateFields)
        binding.actvStreet.afterTextChanged(this::validateFields)
        binding.actvHouse.afterTextChanged(this::validateFields)
        // etApartment.afterTextChanged(this::validateFields)
        binding.tvQrCode.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_inputAddressFragment_to_qrCodeFragment)
        }
    }

    private fun validateFields(text: String) {
        binding.btnCheckAvailableServices.isEnabled =
            binding.actvCity.text.isNotEmpty() && binding.actvStreet.text.isNotEmpty() && binding.actvHouse.text.isNotEmpty()
    }

    private fun getAddress() =
        "${binding.actvCity.text} ${binding.actvStreet.text} ${binding.actvHouse.text}  ${binding.etApartment.text}"
}
