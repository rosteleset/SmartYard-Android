package com.sesameware.smartyard_oem.ui.main.address.addressVerification.office

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.IssueClass
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION2
import com.sesameware.domain.model.response.Office
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 19/03/2020.
 */
class OfficeViewModel(
    private val addressInteractor: AddressInteractor,
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    private val _offices = MutableLiveData<List<Office>>()
    val offices: LiveData<List<Office>>
        get() = _offices

    init {
        getOffices()
    }

    private fun getOffices() {
        viewModelScope.withProgress {
            val res = addressInteractor.getOffices()
            _offices.postValue(res.data)
        }
    }

    /**        """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$. Адрес, введённый пользователем: $адрес$. клиент подойдет в офис для получения подтверждения.
     ""type"": 32
     },
     ""customFields"": {
     ""10011"": ""-1"",
     ""11841"": $телефон, введенный пользователем$,
     ""12440"": ""Приложение"",
     ""10743"": $широта$,
     ""10744"": $долгота$,
     ""10941"": 10580

     },
     ""actions"": [
     ""Начать работу"",
     ""Передать в офис""
     ]
     }"*/

    fun createIssue(address: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName} Адрес, введённый пользователем: $address.\n клиент подойдет в офис для получения подтверждения."
        val x10011 = "-1"
        val x11841 = preferenceStorage.phone
        val x12440 = "Приложение"
        val x10941 = 10580
        super.createIssue(
            summary,
            description,
            address,
            CustomFields(
                x10011 = x10011,
                x11841 = x11841,
                x12440 = x12440,
                x10941 = x10941
            ),
            ACTION2,
            IssueClass.ConfirmAddressInOfficeIssue
        )
    }
}
