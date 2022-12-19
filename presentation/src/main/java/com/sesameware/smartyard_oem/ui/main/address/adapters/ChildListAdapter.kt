package com.sesameware.smartyard_oem.ui.main.address.adapters

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.sesameware.smartyard_oem.ui.main.address.adaterdelegates.EventLogAdapterDelegate
import com.sesameware.smartyard_oem.ui.main.address.adaterdelegates.VideoCameraAdapterDelegate
import com.sesameware.smartyard_oem.ui.main.address.adaterdelegates.YardAdapterDelegate
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem

/**
 * @author Nail Shakurov
 * Created on 2020-02-11.
 */
class ChildListAdapter(
    setting: ParentListAdapterSetting
) : ListDelegationAdapter<List<DisplayableItem>>(
    VideoCameraAdapterDelegate(setting),
    YardAdapterDelegate(setting),
    EventLogAdapterDelegate(setting)
)
