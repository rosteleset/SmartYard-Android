package com.sesameware.domain.interfaces

import com.sesameware.domain.model.NetworkStatus
import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val networkStatus: StateFlow<NetworkStatus>
}