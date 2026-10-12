package com.sesameware.domain.model

sealed interface NetworkStatus {
    data object AvailableWiFi : NetworkStatus
    data object AvailableCellular : NetworkStatus
    data object Undefined : NetworkStatus
    data object Disconnected : NetworkStatus
}