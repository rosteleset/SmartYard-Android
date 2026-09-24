package com.sesameware.smartyard_oem.ui.main.settings.model

data class AccessManagementPayload(
    val address: String,
    val flatId: Int,
    val flatOwner: Boolean,
    val hasGates: Boolean,
    val hasPlog: Boolean,
    val clientId: String
)

fun SettingsAddressModel.toAccessManagementPayload() =
    AccessManagementPayload(address, flatId, flatOwner, hasGates, hasPlog, clientId)