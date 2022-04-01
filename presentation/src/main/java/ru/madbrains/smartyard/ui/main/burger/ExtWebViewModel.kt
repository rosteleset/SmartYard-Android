package ru.madbrains.smartyard.ui.main.burger

import androidx.lifecycle.MutableLiveData
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel

class ExtWebViewModel : GenericViewModel() {
    val onPostRefreshParent = MutableLiveData<Event<Int>>()
}
