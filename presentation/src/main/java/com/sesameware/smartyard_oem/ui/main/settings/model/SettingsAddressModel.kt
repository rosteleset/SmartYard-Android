package com.sesameware.smartyard_oem.ui.main.settings.model

import com.sesameware.domain.model.response.Settings

/**
 * @author Nail Shakurov
 * Created on 2020-02-17.
 */
data class SettingsAddressModel(
    val address: String,
    val contractName: String,
    val houseId: Int,
    val flatId: Int,
    val clientId: String,
    val flatOwner: Boolean,
    val services: List<String>,
    val lcab: String?,
    val hasGates: Boolean,
    val hasPlog: Boolean,
) {
    var isExpanded: Boolean = false
}

fun Settings.toSettingsAddressModel(isExpanded: Boolean = false) =
    SettingsAddressModel(
        address,
        contractName,
        houseId,
        flatId,
        clientId,
        flatOwner,
        services,
        lcab,
        hasGates,
        hasPlog,
    ).apply { this.isExpanded = isExpanded }