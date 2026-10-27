package com.sesameware.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.sesameware.domain.interfaces.NetworkMonitor
import com.sesameware.domain.model.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

internal class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val localScope = CoroutineScope(Dispatchers.IO)

    @OptIn(FlowPreview::class)
    @SuppressLint("MissingPermission")
    override val networkStatus: StateFlow<NetworkStatus> = callbackFlow {
        var ignoreNextAvailable = false

        val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val newStatus = capabilities.toStatus() ?: return

                    // skip cached available status
                    if (ignoreNextAvailable &&
                        (newStatus is NetworkStatus.AvailableWiFi ||
                        newStatus is NetworkStatus.AvailableCellular)
                    ) {
                        ignoreNextAvailable = false
                        return
                    }

                    if (newStatus == NetworkStatus.Disconnected) {
                        ignoreNextAvailable = true
                    }

                    trySend(newStatus)
                }

                override fun onLost(network: Network) {}
            }

            val activeNet = connectivityManager.activeNetwork
            val capabilities = activeNet?.let { connectivityManager.getNetworkCapabilities(it) }
            trySend(capabilities?.toStatus() ?: NetworkStatus.Disconnected)

            connectivityManager.registerDefaultNetworkCallback(callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.onEach {
            delay(500L)
        }.debounce { status ->
            when (status) {
                NetworkStatus.Disconnected -> 600L
                NetworkStatus.AvailableCellular -> 1500L
                else -> 0L
            }
        }.distinctUntilChanged()
        .stateIn(
            localScope,
            SharingStarted.Eagerly,
            NetworkStatus.Undefined
        )

    private fun NetworkCapabilities.toStatus(): NetworkStatus? {
        val hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // connected to gateway
        val isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // System HTTP probe returned 204
        val notSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // by phone call
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
        } else {
            true
        }

        return if (hasInternet && notSuspended && isValidated) {
             when {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    NetworkStatus.AvailableWiFi
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    NetworkStatus.AvailableCellular
                else -> NetworkStatus.Disconnected
            }
        } else {
            null
        }
    }
}