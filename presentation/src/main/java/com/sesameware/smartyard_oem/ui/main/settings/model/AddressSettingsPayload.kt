package com.sesameware.smartyard_oem.ui.main.settings.model

data class AddressSettingsPayload(
    val address: String, 
    val flatId: Int,
    val isKey: Boolean,
    val flatOwner: Boolean,
    val clientId: String
)

fun SettingsAddressModel.toAddressSettingsPayload(isKey: Boolean) =
    AddressSettingsPayload(address, flatId, isKey, flatOwner, clientId)
