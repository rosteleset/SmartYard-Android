package com.sesameware.smartyard_oem.ui.main.address.noNetwork

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

interface NoNetworkDelegate {
    fun extendConfig(
        viewModel: NoNetworkViewModel,
        viewLifecycleOwner: LifecycleOwner,
        navController: NavController,
        serviceList: MutableList<NoNetworkFragment.ItemService>
    )
}
