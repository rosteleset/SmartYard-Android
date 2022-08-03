package ru.madbrains.smartyard.ui.main.address.availableServices

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

/**
 * @author Nail Shakurov
 * Created on 2020-02-13.
*/
class AvailableAdapter(private var clickCheckBox: () -> Unit) : ListDelegationAdapter<List<AvailableModel>>(
    AvailableAdapterDelegate(clickCheckBox)
)
