package com.sesameware.smartyard_oem

import androidx.lifecycle.MutableLiveData
import com.sesameware.domain.model.CommonError

class GlobalDataSource {
    val globalErrorsSink = MutableLiveData<Event<CommonError>>()
    val progressVisibility = MutableLiveData<Boolean>()
}
