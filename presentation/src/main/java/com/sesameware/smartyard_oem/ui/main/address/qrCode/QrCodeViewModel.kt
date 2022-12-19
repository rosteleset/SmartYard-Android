package com.sesameware.smartyard_oem.ui.main.address.qrCode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 24/03/2020.
 */
class QrCodeViewModel(
    private val addressInteractor: AddressInteractor
) : GenericViewModel() {

    val navigationToDialog = MutableLiveData<Event<String>>()

    fun registerQR(url: String) {
        viewModelScope.withProgress {
            val res = addressInteractor.registerQR(url)
            navigationToDialog.postValue(Event(res.data ?: res.message))
        }
    }
}
