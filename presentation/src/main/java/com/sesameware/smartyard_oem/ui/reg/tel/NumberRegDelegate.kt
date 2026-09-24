package com.sesameware.smartyard_oem.ui.reg.tel

import com.sesameware.smartyard_oem.databinding.FragmentNumberRegBinding
import com.sesameware.smartyard_oem.databinding.PinEntryBinding

interface NumberRegDelegate {
    fun extendConfig(binding: FragmentNumberRegBinding, pinSlots: List<PinEntryBinding>)
}