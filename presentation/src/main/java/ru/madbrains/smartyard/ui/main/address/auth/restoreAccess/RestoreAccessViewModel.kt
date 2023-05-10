package ru.madbrains.smartyard.ui.main.address.auth.restoreAccess

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.model.response.Recovery
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel

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
