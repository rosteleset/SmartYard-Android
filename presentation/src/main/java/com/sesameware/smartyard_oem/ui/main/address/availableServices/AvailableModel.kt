package com.sesameware.smartyard_oem.ui.main.address.availableServices

/**
 * @author Nail Shakurov
 * Created on 2020-02-13.
 */
data class AvailableModel(
    var isMandatory: Boolean = false,  // is service mandatory? (if yes user can't uncheck it)
    var isChecked: Boolean = false,  // did user check service?
    var title: String = "",
    var description: String = ""
)
