package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models

data class LicensePlateValue(
    val number: String,
    val countryCode: String = "ru",
    val validFrom: String? = null,
    val validTo: String? = null,
    val watcherId: Int? = null
)
