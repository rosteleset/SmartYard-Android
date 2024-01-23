package com.sesameware.smartyard_oem.ui.main.address.addressVerification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentAddressVerificationBinding
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.courier.CourierFragment
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.office.OfficeFragment
import com.sesameware.smartyard_oem.ui.main.address.noNetwork.NoNetworkFragmentArgs

class AddressVerificationFragment : Fragment() {
    private var _binding: FragmentAddressVerificationBinding? = null
    private val binding get() = _binding!!

    private var address = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddressVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            address = NoNetworkFragmentArgs.fromBundle(it).address
        }
        binding.imageView7.setOnClickListener {
            this.findNavController().popBackStack()
        }
        val adapter = TabAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        adapter.addFragment(
            { CourierFragment.getInstance(address) },
            resources.getString(R.string.address_verification_tab_title_1)
        )
        adapter.addFragment(
            { OfficeFragment.getInstance(address) },
            resources.getString(R.string.address_verification_tab_title_2)
        )
        binding.vpAddressVerification.adapter = adapter
        TabLayoutMediator(binding.tlAddressVerification, binding.vpAddressVerification) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }
}
