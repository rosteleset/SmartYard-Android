package com.sesameware.smartyard_oem.ui.main.address.availableServices

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.sesameware.smartyard_oem.databinding.FragmentAvailableServicesBinding

interface AvailableServicesDelegate {
    fun extendConfig(
        binding: FragmentAvailableServicesBinding,
        viewModel: AvailableServicesViewModel,
        viewLifecycleOwner: LifecycleOwner,
        navController: NavController,
        serviceList: List<AvailableModel>,
        address: String
    )
}
