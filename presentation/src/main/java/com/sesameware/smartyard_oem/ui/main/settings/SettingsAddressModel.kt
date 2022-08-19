package com.sesameware.smartyard_oem.ui.main.settings

/**
 * @author Nail Shakurov
 * Created on 2020-02-17.
 */
data class SettingsAddressModel(
    var address: String,
    var contractName: String,
    var houseId: Int,
    var flatId: Int,
    var clientId: String,
    var contractOwner: Boolean,
    var flatOwner: String,
    var services: List<String>,
    var lcab: String?,
    var hasGates: Boolean,
    var isExpanded: Boolean = false
)
