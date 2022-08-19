package com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.model.response.Recovery
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 20/03/2020.
 */
class RestoreAccessViewModel(
    private val addressInteractor: AddressInteractor
) : GenericViewModel() {

    val recoveryList = MutableLiveData<List<Recovery>>()

    val navigationToCodeSms = MutableLiveData<Event<Unit>>()

    fun recoveryOptions(contract: String) {
        viewModelScope.withProgress {
            val res = addressInteractor.recoveryOptions(contract)
            res?.let {
                recoveryList.postValue(it.data)
            }
        }
    }

    fun sentCodeRecovery(contract: String, contractId: String) {
        viewModelScope.withProgress {
            addressInteractor.sentCodeRecovery(contract, contractId)
            navigationToCodeSms.postValue(Event(Unit))
        }
    }
}
