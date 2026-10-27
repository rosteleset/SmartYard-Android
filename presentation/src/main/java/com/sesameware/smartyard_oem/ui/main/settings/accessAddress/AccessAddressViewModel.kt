package com.sesameware.smartyard_oem.ui.main.settings.accessAddress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.LPRSInteractor
import com.sesameware.domain.model.response.Code
import com.sesameware.domain.model.response.Intercom
import com.sesameware.domain.model.response.LicensePlate
import com.sesameware.domain.model.response.LprsVersion
import com.sesameware.domain.model.response.Plog
import com.sesameware.domain.model.response.Settings.Roommate
import com.sesameware.domain.model.response.SupportedPlateNumberFormat
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

    // LPRS API version centrally determined from the provider configuration
    val lprsVersion: LprsVersion
        get() = DataModule.providerConfig.lprsVersionType

    // Supported license plate number formats (version 2 only)
    private val _supportedPlateFormats = MutableLiveData<List<SupportedPlateNumberFormat>>()
    val supportedPlateFormats: LiveData<List<SupportedPlateNumberFormat>>
        get() = _supportedPlateFormats

    // Signal successful license plate addition (version 2) to close the dialog
    private val _addLicensePlateV2Success = MutableLiveData<Event<Unit>>()
    val addLicensePlateV2Success: LiveData<Event<Unit>> get() = _addLicensePlateV2Success

    fun resetCode(flatId: Int) {
        viewModelScope.withProgress {
            val res = addressInteractor.resetCode(flatId)
            _resetCode.postValue(res.data)
        }
    }

    fun guestAccess(flatId: Int, isOpen: Boolean, guestAccessHours: Int = 1) {
        val cal: Calendar = Calendar.getInstance()
        if (isOpen) {
            cal.add(Calendar.HOUR_OF_DAY, guestAccessHours)
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
            lprsInteractor.addLicensePlate(flatId, LicensePlate(licensePlate.number))
            refreshLicensePlateList(flatId)
        }
    }

    /**
     * Adds a license plate number using API version 2, with a country code and an optional validity period.
     * On success, signals the dialog to close and refresh the list.
     */
    fun addLicensePlateV2(
        flatId: Int,
        number: String,
        countryCode: String,
        validFrom: String?,
        validTo: String?,
        trackEvent: Boolean
    ) {
        viewModelScope.withProgress {
            lprsInteractor.addLicensePlateV2(flatId, number, countryCode, validFrom, validTo)
            if (trackEvent) {
                addressInteractor.trackEvent(flatId, Plog.EVENT_OPEN_GATES_BY_VEHICLE, number, "")
            }
            refreshLicensePlateList(flatId)
            _addLicensePlateV2Success.postValue(Event(Unit))
        }
    }

    /**
     * Loads the supported license plate number formats (list of countries) for API version 2.
     */
    fun loadSupportedPlateFormats() {
        viewModelScope.withProgress {
            val formats = lprsInteractor.listSupportedPlateNumberFormats()?.data ?: listOf()
            _supportedPlateFormats.postValue(formats)
        }
    }

    private suspend fun refreshLicensePlateList(flatId: Int) {
        val plates = when (lprsVersion) {
            LprsVersion.V2 -> lprsInteractor.listLicensePlatesV2(flatId)?.data
            else -> lprsInteractor.listLicensePlates(flatId)?.data
        }
        _licensePlates.postValue(plates ?: listOf())
    }

    /**
     * Removes a license plate number and refreshes the list.
     * [onRemoved] is invoked only if the removal succeeded, once the whole operation has completed.
     */
    fun removeLicensePlate(flatId: Int, number: String, onRemoved: (() -> Unit)? = null) {
        var removed = false
        viewModelScope.withProgress {
            lprsInteractor.removeLicensePlate(flatId, number)
            removed = true
            refreshLicensePlateList(flatId)
        }.invokeOnCompletion {
            if (removed) onRemoved?.invoke()
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
        if (dataIntercom.data.lprsDisabled == false) {
            refreshLicensePlateList(flatId)
        }
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
