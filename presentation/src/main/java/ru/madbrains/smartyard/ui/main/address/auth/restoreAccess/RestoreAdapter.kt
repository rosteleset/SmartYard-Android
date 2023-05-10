package ru.madbrains.smartyard.ui.main.address.auth.restoreAccess

import android.app.Activity
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

/**
 * @author Nail Shakurov
 * Created on 20/03/2020.
 */

class RestoreAdapter(
    activity: Activity,
    clickListener: (position: Int, id: String, name: String) -> Unit
) : ListDelegationAdapter<List<RecoveryModel>>(
    RestoreAdapterDelegate(activity, clickListener)
)
