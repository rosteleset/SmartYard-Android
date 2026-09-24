package com.sesameware.smartyard_oem.ui.main.settings.addressSettings

import com.sesameware.smartyard_oem.databinding.FragmentAddressSettingsBinding

interface AddressSettingsDelegate {
    fun extendConfig(binding: FragmentAddressSettingsBinding, showDialogDelete: () -> Unit?)
}