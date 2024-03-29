package com.sesameware.smartyard_oem.ui.reg.providers

import android.app.Activity
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

class ProvidersAdapter(
    activity: Activity,
    clickListener: (id: String, providerName: String, baseUrl: String) -> Unit
) : ListDelegationAdapter<List<ProviderModel>>(
    ProvidersAdapterDelegate(activity, clickListener)
)
