package com.sesameware.smartyard_oem.ui.main.address.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.IssueTypeV2
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 17/03/2020.
 */
class AuthViewModel(
    private val addressInteractor: AddressInteractor,
    issueInteractor: IssueInteractor,
    geoInteractor: GeoInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    private val _navigationToAddressAction = MutableLiveData<Event<Unit>>()
    val navigationToAddressAction: LiveData<Event<Unit>>
        get() = _navigationToAddressAction

    fun signIn(contractNumber: String, password: String) {
        viewModelScope.withProgress {
            addressInteractor.addMyPhone(contractNumber, password, null, null)
            _navigationToAddressAction.postValue(Event(Unit))
        }
    }

    fun createIssue() {
        if (DataModule.providerConfig.issuesVersion != "2") {
            createIssueV1()
        } else {
            createIssueV2()
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

    private fun createIssueV1() {
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

    private fun createIssueV2() {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.REQUEST_CREDENTIALS,
            userName = preferenceStorage.sentName.toString()
        )
        super.createIssueV2(issue)
    }

    fun seenWarning() {
        preferenceStorage.whereIsContractWarningSeen = true
    }
}
