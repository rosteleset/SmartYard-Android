package com.sesameware.smartyard_oem.ui.main.settings.accessAddress

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.model.response.Code
import com.sesameware.domain.model.response.Intercom
import com.sesameware.domain.model.response.Settings.Roommate
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
class AccessAddressViewModel(
    private val addressInteractor: AddressInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    var intercom = MutableLiveData<Intercom?>()

    var resetCode = MutableLiveData<Code?>()

    var roommate = MutableLiveData<List<Roommate>>()

    var operationRoommate = MutableLiveData<Unit>()

    var dialogToSuccessSms = MutableLiveData<Event<Unit>>()

    fun resetCode(flatId: Int) {
        viewModelScope.withProgress {
            val res = addressInteractor.resetCode(flatId)
            resetCode.postValue(res.data)
        }
    }

    fun guestAccess(flatId: Int, isOpen: Boolean) {
        val cal: Calendar = Calendar.getInstance()
        if (isOpen) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        } else {
            cal.add(Calendar.SECOND, -1)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = cal.timeZone
        val autoOpen = dateFormat.format(cal.time)
        viewModelScope.withProgress {
            val res = addressInteractor.putIntercom(
                flatId,
                null,
                null,
                null,
                autoOpen,
                null,
                null,
                null,
                null,
                null
            )
            intercom.postValue(res.data)
        }
    }

    fun getRoommateAndIntercom(flatId: Int) {
        viewModelScope.withProgress {
            val roommate = addressInteractor.getRoommate()
            val roommateResponse = roommate.data.first { it.flatId == flatId }
            this@AccessAddressViewModel.roommate.postValue(roommateResponse.roommates)

            mPreferenceStorage.xDmApiRefresh = true
            val dataIntercom = addressInteractor.getIntercom(flatId)
            intercom.postValue(dataIntercom.data)
        }
    }

    fun addRoommate(flatId: Int, number: String, type: String) {
        viewModelScope.withProgress {
            addressInteractor.access(flatId, number, type, null, null)
            operationRoommate.postValue(Unit)
        }
    }

    fun deleteRoommate(flatId: Int, number: String, clientId: String) {
        viewModelScope.withProgress {
            addressInteractor.access(flatId, number, null, "2001-01-01 00:00:00", clientId)
            operationRoommate.postValue(Unit)
        }
    }

    fun resend(flatId: Int, number: String) {
        viewModelScope.withProgress {
            addressInteractor.resend(flatId, number)
            dialogToSuccessSms.value = Event(Unit)
        }
    }
}
