package com.sesameware.smartyard_oem.ui.main.address.availableServices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 21/04/2020.
 */
class AvailableServicesViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    private val _navigateToAddressVerificationFragmentAction = MutableLiveData<Event<Unit>>()
    val navigateToAddressVerificationFragmentAction: LiveData<Event<Unit>>
        get() = _navigateToAddressVerificationFragmentAction

    private val _stateEnabledButtonNext = MutableLiveData<Event<Boolean>>()
    val stateEnabledButtonNext: LiveData<Event<Boolean>>
        get() = _stateEnabledButtonNext

    fun avalaibleOkayBtn(servicesList: MutableList<AvailableModel>) {
        if (servicesList.none { !it.active } && servicesList.none { it.active && it.check })
            _stateEnabledButtonNext.value = Event(false)
        else _stateEnabledButtonNext.value = Event(true)
    }

    fun checkServices(servicesList: MutableList<AvailableModel>, address: String) {
        issueOnlyService(
            address,
            servicesList.filter { it.active && it.check }.joinToString { it -> "\'${it.title}\'" }
        )
    }

    /**    """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$\nТелефон: $телефон$\nАдрес, введённый пользователем: $адрес$.\nПодключение услуг(и): $перечень выбранного$.\n Выполнить звонок клиенту и осуществить консультацию
     ""type"": 32
     },
     ""customFields"": {
     ""10011"": ""-1"",
     ""11841"": $телефон, введенный пользователем$,
     ""12440"": ""Приложение"",
     ""10743"": $широта$,
     ""10744"": $долгота$,
     },
     ""actions"": [
     ""Начать работу"",
     ""Позвонить""
     ]
     }"*/
    private fun issueOnlyService(address: String, connectedServicesText: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}.\n Телефон: ${preferenceStorage.phone}.\n Адрес, введённый пользователем: $address.\n" +
                "Подключение услуг(и): $connectedServicesText.\n Выполнить звонок клиенту и осуществить консультацию"
        val x10011 = "-1"
        val x11841 = preferenceStorage.phone
        val x12440 = "Приложение"
        super.createIssue(
            summary,
            description,
            address,
            CustomFields(
                x10011 = x10011,
                x11841 = x11841,
                x12440 = x12440
            ),
            ACTION1
        )
    }
}
