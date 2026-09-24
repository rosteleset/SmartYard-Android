package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

import androidx.lifecycle.LifecycleOwner
import com.sesameware.smartyard_oem.databinding.FragmentCityCameraBinding

interface CityCameraDelegate {
    fun extendConfig(
        binding: FragmentCityCameraBinding,
        viewModel: CityCamerasViewModel,
        viewLifecycleOwner: LifecycleOwner
    )
}