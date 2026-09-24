package com.sesameware.smartyard_oem.ui.main.address

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.databinding.FragmentAddressBinding
import com.sesameware.smartyard_oem.ui.main.address.models.IssueAction

interface AddressDelegate {
    fun extendConfig(
        navController: NavController,
        binding: FragmentAddressBinding,
        viewModel: AddressViewModel,
        viewLifecycleOwner: LifecycleOwner,
        onScrollListener: RecyclerView.OnScrollListener?
    )

    fun onIssueAction(navController: NavController, action: IssueAction): Boolean = false
}
