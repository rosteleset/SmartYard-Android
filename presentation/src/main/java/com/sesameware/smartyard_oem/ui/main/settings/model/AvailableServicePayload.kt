package com.sesameware.smartyard_oem.ui.main.settings.model

import com.sesameware.domain.model.Services

data class AvailableServicePayload(
    val serviceType: Services,
    val model: SettingsAddressModel,
    val isConnected: Boolean
)
