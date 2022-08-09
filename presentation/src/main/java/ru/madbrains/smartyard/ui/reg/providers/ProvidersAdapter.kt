package ru.madbrains.smartyard.ui.reg.providers

import android.app.Activity
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

class ProvidersAdapter(
    activity: Activity,
    clickListener: (id: String, baseUrl: String) -> Unit
) : ListDelegationAdapter<List<ProviderModel>>(
    ProvidersAdapterDelegate(activity, clickListener)
)
