package com.sesameware.smartyard_oem.ui.main.burger

import androidx.lifecycle.MutableLiveData
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

class ExtWebViewModel : GenericViewModel() {
    val onPostRefreshParent = MutableLiveData<Event<Int>>()
}
