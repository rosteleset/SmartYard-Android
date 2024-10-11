package ru.madbrains.smartyard.ui.main.address.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.model.request.CreateIssuesRequest.CustomFields
import ru.madbrains.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import ru.madbrains.domain.model.response.AddMyPhoneResponse
import ru.madbrains.domain.model.response.CheckOffertaItem
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.ui.main.BaseIssueViewModel
import timber.log.Timber
import java.security.PrivateKey

/**
 * @author Nail Shakurov
 * Created on 17/03/2020.
 */
class AuthViewModel(
    private val addressInteractor: AddressInteractor,
    issueInteractor: IssueInteractor,
    geoInteractor: GeoInteractor,
    private val preferenceStorage: PreferenceStorage,
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    private val _navigationToOffertaByAddressAction = MutableLiveData<Event<Unit>>()
    val navigationToOffertaByAddressAction: LiveData<Event<Unit>>
        get() = _navigationToOffertaByAddressAction


    private val _navigationToAddressAction = MutableLiveData<Event<Unit>>()
    val navigationToAddressAction: LiveData<Event<Unit>>
        get() = _navigationToAddressAction

    private val _navigationByAddressAction = MutableLiveData<Event<Unit>>()
    val navigationByAddressAction: LiveData<Event<Unit>>
        get() = _navigationByAddressAction

    private val _navigationToOffertaAction = MutableLiveData<Event<Unit>>()
    val navigationToOffertaAction: LiveData<Event<Unit>>
        get() = _navigationToOffertaAction

    private val _offertaList = MutableLiveData<List<CheckOffertaItem>>()
    val offertaList: LiveData<List<CheckOffertaItem>>
        get() = _offertaList

    private val _isOffertaApply = MutableLiveData<Boolean>()
    val isOffertaApply: LiveData<Boolean>
        get() = _isOffertaApply




    fun signIn(contractNumber: String, password: String) {
        viewModelScope.withProgress {
            val res = addressInteractor.addMyPhone(contractNumber, password, null, null)!!
            if (res.code == 204) {
                _navigationToAddressAction.postValue(Event(Unit))
            }
        }
    }

    fun setOffertaApply(isAppply: Boolean){
        _isOffertaApply.value = isAppply
    }

    private fun setOffertaList(list: List<CheckOffertaItem>){
        _offertaList.value = list
    }

    fun checkOfferta(contractNumber: String, password: String) {
        viewModelScope.withProgress {
            val res = addressInteractor.checkOfferta(contractNumber, password)
            if (res?.code == 200) {
                if (res.data.isEmpty()) {
                    _navigationToAddressAction.postValue(Event(Unit))
                } else {
                    val data = res.data
                    setOffertaList(data)
                    _navigationToOffertaAction.postValue(Event(Unit))
                }
            }
        }
    }

    fun checkOffertaByAddress(houseId: Int, flat: Int) {
        viewModelScope.withProgress {
            val res = addressInteractor.checkOffertaByAddress(houseId, flat)
            Timber.d("RESPONSETCHEOKOIFE ${res?.code} ${res?.data}")
            if (res?.code == 200) {
                if (res.data.isEmpty()) {
                    _navigationByAddressAction.postValue(Event(Unit))
                } else {
                    val data = res.data
                    setOffertaList(data)
                    _navigationToOffertaByAddressAction.postValue(Event(Unit))
                }
            }
        }
    }

    fun acceptOffertaByAddress(houseId: Int, flat: Int){
        viewModelScope.withProgress {
            val res = addressInteractor.acceptOffertaByAddress(houseId, flat)
            if (res?.code == 204){
                _navigationToAddressAction.postValue(Event(Unit))
            }
        }
    }

    fun acceptOfferta(contractNumber: String, password: String) {
        viewModelScope.withProgress {
            val res = addressInteractor.acceptOfferta(contractNumber, password)
            if (res?.code == 204) {
                _navigationToAddressAction.postValue(Event(Unit))
            }
        }
    }


    /**
    """issue"": {
    ""project"": ""REM"",
    ""summary"": ""Авто: Звонок с приложения"",
    ""description"":Выполнить звонок клиенту для напоминания номера договора и пароля от личного кабинета"".
    ""type"": 32
    },
    ""customFields"": {
    ""10011"": ""-3"",
    ""11841"": $телефон, введенный пользователем$,
    ""12440"": ""Приложение"",
    },
    ""actions"": [
    ""Начать работу"",
    ""Позвонить""
    ]
    }"
     **/

    fun createIssue() {
        val description =
            "Выполнить звонок клиенту для напоминания номера договора и пароля от личного кабинета."
        val summary = "Авто: Звонок с приложения"
        val x10011 = "-3"
        val x11841 = preferenceStorage.phone
        val x12440 = "Приложение"
        super.createIssue(
            summary,
            description,
            null,
            CustomFields(
                x10011 = x10011,
                x11841 = x11841,
                x12440 = x12440
            ),
            ACTION1
        )
    }

    fun seenWarning() {
        preferenceStorage.whereIsContractWarningSeen = true
    }
}
