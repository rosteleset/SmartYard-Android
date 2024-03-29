package com.sesameware.smartyard_oem.ui.main.address.inputAdress

import android.R.layout
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.domain.model.response.HousesData
import com.sesameware.domain.model.response.LocationData
import com.sesameware.domain.model.response.StreetsData
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.afterTextChanged
import com.sesameware.smartyard_oem.databinding.FragmentInputAddressBinding
import com.sesameware.smartyard_oem.hideKeyboard

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
        binding.actvCity.setOnItemClickListener() { parent, _, position, _ ->
            val selectItem = parent.adapter.getItem(position) as LocationData
            binding.actvCity.setText(selectItem.name)
            mViewModel.getStreet(selectItem.locationId)
        }

        streetAdapter =
            StreetAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvStreet.setAdapter(streetAdapter)
        binding.actvStreet.setOnItemClickListener() { parent, _, position, _ ->
            val selectedItem = parent.adapter.getItem(position) as StreetsData?
            binding.actvStreet.setText(selectedItem?.name)
            mViewModel.getHouses(selectedItem?.streetId ?: 0)
        }

        houseAdapter =
            HousesAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvHouse.setAdapter(houseAdapter)
        binding.actvHouse.setOnItemClickListener() { parent, _, position, _ ->
            val selectedItem = parent.adapter.getItem(position) as HousesData?
            binding.actvHouse.setText(selectedItem?.number)
        }
    }

    @Deprecated("Deprecated in Java")
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
            viewLifecycleOwner
        ) {
            cityAdapter.addData(it)
        }

        mViewModel.progressCity.observe(
            viewLifecycleOwner
        ) {
            binding.progressCity.isVisible = it
        }

        mViewModel.progressStreet.observe(
            viewLifecycleOwner
        ) {
            binding.progressStreet.isVisible = it
        }

        mViewModel.progressHouse.observe(
            viewLifecycleOwner
        ) {
            binding.progressHouse.isVisible = it
        }

        mViewModel.streetList.observe(
            viewLifecycleOwner
        ) {
            streetAdapter.addData(it)
        }

        mViewModel.houseList.observe(
            viewLifecycleOwner
        ) {
            houseAdapter.addData(it)
        }

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
        binding.actvHouse.afterTextChanged(this::validateHouse)
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

    private fun validateHouse(text: String) {
        houseId = null
        mViewModel.houseList.value?.forEach {
            if (it.number == text) {
                houseId = it.houseId
                return@forEach
            }
        }

        validateFields(text)
    }

    private fun getAddress() =
        "${binding.actvCity.text} ${binding.actvStreet.text} ${binding.actvHouse.text}  ${binding.etApartment.text}"
}
