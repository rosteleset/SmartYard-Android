package ru.madbrains.smartyard

import androidx.lifecycle.MutableLiveData
import ru.madbrains.domain.model.CommonError

class GlobalDataSource {
    val globalErrorsSink = MutableLiveData<Event<CommonError>>()
    val progressVisibility = MutableLiveData<Boolean>()
}
