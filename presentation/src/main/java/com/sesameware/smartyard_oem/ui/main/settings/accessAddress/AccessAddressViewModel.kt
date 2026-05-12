package com.sesameware.smartyard_oem.ui.main.settings.accessAddress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.LPRSInteractor
import com.sesameware.domain.model.response.Code
import com.sesameware.domain.model.response.Intercom
import com.sesameware.domain.model.response.LicensePlate
import com.sesameware.domain.model.response.Settings.Roommate
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
class AccessAddressViewModel(
    private val addressInteractor: AddressInteractor,
    private val lprsInteractor: LPRSInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    private val _intercom = MutableLiveData<Intercom?>()
    val intercom: LiveData<Intercom?> get() = _intercom

    private val _resetCode = MutableLiveData<Code?>()
    val resetCode: LiveData<Code?> get() = _resetCode

    private val _roommate = MutableLiveData<List<Roommate>>(listOf())
    val roommate: LiveData<List<Roommate>> get() = _roommate

    private val _licensePlates = MutableLiveData<List<LicensePlate>>(listOf())
    val licensePlates: LiveData<List<LicensePlate>> get() =
        _licensePlates.distinctUntilChanged()

    private val _dialogToSuccessSms = MutableLiveData<Event<Unit>>()
    val dialogToSuccessSms: LiveData<Event<Unit>> get() = _dialogToSuccessSms

    fun resetCode(flatId: Int) {
        viewModelScope.withProgress {
            val res = addressInteractor.resetCode(flatId)
            _resetCode.postValue(res.data)
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
            _intercom.postValue(res.data)
        }
    }

    fun addLicensePlate(flatId: Int, licensePlate: LicensePlateValue) {
        viewModelScope.withProgress {
            lprsInteractor.addLicensePlate(flatId, LicensePlate(licensePlate.value))
            refreshLicensePlateList(flatId)
        }
    }

    private suspend fun refreshLicensePlateList(flatId: Int) {
        lprsInteractor.listLicensePlates(flatId)?.data?.let {
            _licensePlates.postValue(it)
        }
    }

    fun getLicensePlateList(flatId: Int) {
        viewModelScope.withProgress {
            refreshLicensePlateList(flatId)
        }
    }

    fun removeLicensePlate(flatId: Int, licensePlate: LicensePlateValue) {
        viewModelScope.withProgress {
            lprsInteractor.removeLicensePlate(flatId, LicensePlate(licensePlate.value))
            refreshLicensePlateList(flatId)
        }
    }

    fun getRoommateAndIntercom(flatId: Int) {
        viewModelScope.withProgress {
            refreshRoommateAndIntercom(flatId)
        }
    }

    private suspend fun refreshRoommateAndIntercom(flatId: Int) {
        val roommate = addressInteractor.getRoommate()
        val roommateResponse = roommate.data.first { it.flatId == flatId }
        _roommate.postValue(roommateResponse.roommates)

        mPreferenceStorage.xDmApiRefresh = true
        val dataIntercom = addressInteractor.getIntercom(flatId)
        _intercom.postValue(dataIntercom.data)
    }

    fun addRoommate(flatId: Int, number: String, type: String) {
        viewModelScope.withProgress {
            addressInteractor.access(flatId, number, type, null, null)
            refreshRoommateAndIntercom(flatId)
        }
    }

    fun deleteRoommate(flatId: Int, number: String, clientId: String) {
        viewModelScope.withProgress {
            addressInteractor.access(flatId, number, null, "2001-01-01 00:00:00", clientId)
            refreshRoommateAndIntercom(flatId)
        }
    }

    fun resend(flatId: Int, phoneNumber: String) {
        viewModelScope.withProgress {
            addressInteractor.resend(flatId, phoneNumber)
            _dialogToSuccessSms.value = Event(Unit)
        }
    }
}
