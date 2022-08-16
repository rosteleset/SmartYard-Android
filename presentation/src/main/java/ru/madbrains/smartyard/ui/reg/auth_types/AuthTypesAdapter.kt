package ru.madbrains.smartyard.ui.reg.auth_types

import android.app.Activity
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

class AuthTypesAdapter(
    activity: Activity,
    clickListener: (methodId: String) -> Unit
) : ListDelegationAdapter<List<AuthTypesModel>>(
    AuthTypesAdapterDelegate(activity, clickListener)
)
