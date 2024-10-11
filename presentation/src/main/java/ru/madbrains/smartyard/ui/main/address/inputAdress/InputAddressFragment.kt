package ru.madbrains.smartyard.ui.main.address.inputAdress

import android.R.layout
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.response.HousesData
import ru.madbrains.domain.model.response.LocationData
import ru.madbrains.domain.model.response.StreetsData
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.afterTextChanged
import ru.madbrains.smartyard.databinding.FragmentInputAddressBinding
import ru.madbrains.smartyard.hideKeyboard
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.auth.AuthViewModel
import ru.madbrains.smartyard.ui.main.address.auth.offerta.AcceptOffertaFragment
import ru.madbrains.smartyard.ui.main.address.availableServices.AvailableServicesFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVTrimmerFragment
import ru.madbrains.smartyard.ui.main.address.noNetwork.NoNetworkFragment
import timber.log.Timber

class InputAddressFragment : Fragment() {
    private var _binding: FragmentInputAddressBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<InputAddressViewModel>()

    private val mAuthViewModel by sharedViewModel<AuthViewModel>()

    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()

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
        binding.actvCity.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val inputText = binding.actvCity.text.toString().trim().lowercase()
                val cityList = mViewModel.cityList.value
                cityList?.forEach {
                    val nameCity = it.name.trim().lowercase()
                    val locationId = it.locationId
                    if (nameCity == inputText) {
                        binding.actvCity.setText(it.name)
                        mViewModel.getStreet(locationId)
                    }
                }

            }
        }

        streetAdapter =
            StreetAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvStreet.setAdapter(streetAdapter)
        binding.actvStreet.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as StreetsData?
            binding.actvStreet.setText(selectedItem?.name)
            mViewModel.getHouses(selectedItem?.streetId ?: 0)
        }

        binding.actvStreet.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val inputText = binding.actvStreet.text.toString().trim().lowercase()
                val streetList = mViewModel.streetList.value
                streetList?.forEach {
                    val nameStreet = it.name.trim().lowercase()
                    val locationId = it.streetId
                    if (nameStreet == inputText) {
                        binding.actvStreet.setText(it.name)
                        mViewModel.getHouses(locationId)
                    }
                }
            }
        }

        houseAdapter =
            HousesAdapter(requireContext(), layout.simple_list_item_1, mutableListOf())
        binding.actvHouse.setAdapter(houseAdapter)
        binding.actvHouse.setOnItemClickListener() { parent, _, position, id ->
            val selectedItem = parent.adapter.getItem(position) as HousesData?
            binding.actvHouse.setText(selectedItem?.number)
            houseId = selectedItem?.houseId
        }

        binding.actvHouse.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val inputText = binding.actvHouse.text.toString().trim().lowercase()
                val streetList = mViewModel.houseList.value
                streetList?.forEach {
                    val number = it.number.trim().lowercase()
                    val hId = it.houseId
                    if (number == inputText) {
                        houseId = hId
                    }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAutoCompleteTextView()

        binding.ivBack.setOnClickListener {
//            this.findNavController().popBackStack()
            parentFragmentManager.popBackStack()
        }

        binding.btnCheckAvailableServices.setOnClickListener {
            hideKeyboard(requireActivity())
            val flat = binding.etApartment.text.toString().toIntOrNull() ?: 0
            mAuthViewModel.checkOffertaByAddress(houseId ?: -1, flat)
        }

        mAuthViewModel.navigationByAddressAction.observe(
            viewLifecycleOwner,
            EventObserver {
                mViewModel.getServices(houseId = houseId, address = getAddress())
            })

        mAuthViewModel.navigationToOffertaByAddressAction.observe(
            viewLifecycleOwner,
            EventObserver {
                val transaction = parentFragmentManager.beginTransaction()
                val newFragment = AcceptOffertaFragment()
                val bundle = Bundle()
                bundle.putInt("houseId", houseId ?: -1)
                bundle.putInt("flat", binding.etApartment.text.toString().toInt())
                newFragment.arguments = bundle
                transaction.replace(R.id.sv_fragment_input_address, newFragment)
                transaction.addToBackStack("AcceptOffertaFragment")
                transaction.commit()
            })

        mViewModel.navigationToNoNetwork.observe(
            viewLifecycleOwner,
            EventObserver {
//                val action =
//                    InputAddressFragmentDirections.actionInputAddressFragmentToNoNetworkFragment(it)
//                this.findNavController().navigate(action)
                val transaction = parentFragmentManager.beginTransaction()
                val newFragment = NoNetworkFragment()
                val bundle = Bundle()
                bundle.putString("address", it)
                newFragment.arguments = bundle
                transaction.add(R.id.cl_input_address_fragment, newFragment)
                transaction.addToBackStack("NoNetworkFragment")
                transaction.commit()
            }
        )

        mViewModel.navigationToAddingAddress.observe(
            viewLifecycleOwner,
            EventObserver {
                val fragmentCount = parentFragmentManager.backStackEntryCount
                for (i in 0 until fragmentCount) {
                    parentFragmentManager.popBackStack()
                }
                mainActivityViewModel.bottomNavigateToIntercom()
                val intentBroadcast = Intent(AddressWebViewFragment.REFRESH_INTENT)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentBroadcast)
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
//                val action =
//                    InputAddressFragmentDirections.actionInputAddressFragmentToAvailableServicesFragment(
//                        it.toTypedArray(),
//                        address
//                    )
//                this.findNavController().navigate(action)


                val transaction = parentFragmentManager.beginTransaction()
                val newFragment = AvailableServicesFragment()
                val bundle = Bundle()
                bundle.putString("address", address)
                bundle.putParcelableArray("servicesList", it.toTypedArray())
                newFragment.arguments = bundle
                transaction.add(R.id.cl_input_address_fragment, newFragment)
                transaction.addToBackStack("InputAddressFragment")
                transaction.commit()
            }
        )
        binding.actvCity.afterTextChanged(this::validateFields)
        binding.actvStreet.afterTextChanged(this::validateFields)
        binding.actvHouse.afterTextChanged(this::validateFields)
        // etApartment.afterTextChanged(this::validateFields)
            //TODO Не используется QR
//        binding.tvQrCode.setOnClickListener {
//            NavHostFragment.findNavController(this)
//                .navigate(R.id.action_inputAddressFragment_to_qrCodeFragment)
//        }
    }

    private fun validateFields(text: String) {
        binding.btnCheckAvailableServices.isEnabled =
            binding.actvCity.text.isNotEmpty() && binding.actvStreet.text.isNotEmpty() && binding.actvHouse.text.isNotEmpty()
    }

    private fun getAddress() =
        "${binding.actvCity.text}, ${binding.actvStreet.text}, ${binding.actvHouse.text}, ${binding.etApartment.text}"
}
