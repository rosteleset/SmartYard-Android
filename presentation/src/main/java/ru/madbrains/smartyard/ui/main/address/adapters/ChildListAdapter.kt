package ru.madbrains.smartyard.ui.main.address.adapters

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import ru.madbrains.smartyard.ui.main.address.adaterdelegates.EventLogAdapterDelegate
import ru.madbrains.smartyard.ui.main.address.adaterdelegates.VideoCameraAdapterDelegate
import ru.madbrains.smartyard.ui.main.address.adaterdelegates.YardAdapterDelegate
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem

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
