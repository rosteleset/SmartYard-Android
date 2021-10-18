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
import kotlinx.android.synthetic.main.fragment_input_address.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.response.HousesData
import ru.madbrains.domain.model.response.LocationData
import ru.madbrains.domain.model.response.StreetsData
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.afterTextChanged
import ru.madbrains.smartyard.hideKeyboard

class InputAddressFragment : Fragment() {

    private val mViewModel by viewModel<InputAddressViewModel>()

    private lateinit var cityAdapter: CityAdapter

    private lateinit var streetAdapter: StreetAdapter

    private lateinit var houseAdapter: HousesAdapter

    private var houseId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_input_address, container, false)

    private fun initAutoCompleteTextView() {
        cityAdapter = CityAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        actvCity.setAdapter(cityAdapter)
        actvCity.setOnItemClickListener() { parent, _, position, id ->
            val selectItem = parent.adapter.getItem(position) as LocationData
            actvCity.setText(selectItem.name)
            mViewModel.getStreet(selectItem.locationId)
        }

        streetAdapter =
            StreetAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        actvStreet.setAdapter(streetAdapter)
        actvStreet.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as StreetsData?
            actvStreet.setText(selectedItem?.name)
            mViewModel.getHouses(selectedItem?.streetId ?: 0)
        }

        houseAdapter =
            HousesAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        actvHouse.setAdapter(houseAdapter)
        actvHouse.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as HousesData?
            actvHouse.setText(selectedItem?.number)
            houseId = selectedItem?.houseId
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAutoCompleteTextView()

        ivBack?.setOnClickListener {
            this.findNavController().popBackStack()
        }

        btnСheckAvailableServices?.setOnClickListener {
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
                progressCity.isVisible = it
            }
        )

        mViewModel.progressStreet.observe(
            viewLifecycleOwner,
            Observer {
                progressStreet.isVisible = it
            }
        )

        mViewModel.progressHouse.observe(
            viewLifecycleOwner,
            Observer {
                progressHouse.isVisible = it
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
                progressCity.isVisible = false
                progressStreet.isVisible = false
                progressHouse.isVisible = false
            }
        )

        mViewModel.servicesList.observe(
            viewLifecycleOwner,
            EventObserver {
                val address = if (etApartment.text.isNotEmpty())
                    "${actvCity.text}, ${actvStreet.text}, ${actvHouse.text}, ${etApartment.text}" else
                    "${actvCity.text}, ${actvStreet.text}, ${actvHouse.text}"
                val action =
                    InputAddressFragmentDirections.actionInputAddressFragmentToAvailableServicesFragment(
                        it.toTypedArray(),
                        address
                    )
                this.findNavController().navigate(action)
            }
        )
        actvCity.afterTextChanged(this::validateFields)
        actvStreet.afterTextChanged(this::validateFields)
        actvHouse.afterTextChanged(this::validateFields)
        // etApartment.afterTextChanged(this::validateFields)
        tvQrCode.setOnClickListener {
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_inputAddressFragment_to_qrCodeFragment)
        }
    }

    fun validateFields(text: String) {
        btnСheckAvailableServices?.isEnabled =
            actvCity.text.isNotEmpty() && actvStreet.text.isNotEmpty() && actvHouse.text.isNotEmpty()
    }

    private fun getAddress() =
        "${actvCity.text} ${actvStreet.text} ${actvHouse.text}  ${etApartment.text}"
}
