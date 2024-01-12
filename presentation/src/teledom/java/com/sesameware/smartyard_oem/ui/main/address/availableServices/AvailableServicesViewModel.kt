package com.sesameware.smartyard_oem.ui.main.address.availableServices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.IssueTypeV2
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel
import timber.log.Timber

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
        // Количество общедомовых сервисов
        val commonHouse = servicesList.filter { !it.active }.size

        // Количество выбранных других сервисов
        val otherSelect = servicesList.filter { it.active && it.check }.size

        /**  TODO: По 22 экрану.
         Тут возможно 3 кейса:
         1) Если есть общедомовые сервисы и другие сервисы НЕ выбраны - переходим на экран выбора способа подтверждения курьер / офис.
         2) Если есть общедомовые услуги и выбран какой-либо другой сервис - делаем заявку "подойду в офис самостоятельно"
         3) Если НЕТ общедомовых услуг и выбран какой-либо сервис - делаем заявку "заявка только на услугу"
         Общедомовые это те, которые серым выделены*/
        if (commonHouse > 0 && otherSelect == 0) {
            // 1)
            _navigateToAddressVerificationFragmentAction.value = Event(Unit)
        } else if (commonHouse > 0) {
            // 2)
            issueGoToTheOfficeMy(
                address,
                servicesList.filter { it.active && it.check }.joinToString { it -> "\'${it.title}\'" }
            )
        } else if (otherSelect > 0) {
            // 3)
            issueOnlyService(
                address,
                servicesList.filter { it.active && it.check }.joinToString { it -> "\'${it.title}\'" }
            )
        }
    }

    private fun issueOnlyService(address: String, connectedServicesText: String) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            issueOnlyServiceV1(address, connectedServicesText)
        } else {
            issueOnlyServiceV2(address, connectedServicesText)
        }
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
    private fun issueOnlyServiceV1(address: String, connectedServicesText: String) {
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

    private fun issueOnlyServiceV2(address: String, connectedServicesText: String) {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.CONNECT_SERVICES_NO_COMMON,
            userName = preferenceStorage.sentName.toString(),
            inputAddress = address,
            services = connectedServicesText
        )
        super.createIssueV2(issue)
    }

    private fun issueGoToTheOfficeMy(address: String, connectedServicesText: String) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            issueGoToTheOfficeMyV1(address, connectedServicesText)
        } else {
            issueGoToTheOfficeMyV2(address, connectedServicesText)
        }
    }

    /**    """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$. Адрес, введённый пользователем: $адрес$.\n$список услуг$ Требуется подтверждение адреса и подключение выбранных услуг
     ""type"": 32
     },
     ""customFields"": {
     ""10011"": ""-1"",
     ""11841"": $телефон, введенный пользователем$,
     ""12440"": ""Приложение"",
     ""10743"": $широта$,
     ""10744"": $долгота$,
     ""10941"": 10581
     },
     ""actions"": [
     ""Начать работу"",
     ""Позвонить""
     ]
     }"*/
    private fun issueGoToTheOfficeMyV1(address: String, connectedServicesText: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}\n Адрес, введённый пользователем: $address.\n $connectedServicesText \n Требуется подтверждение адреса и подключение выбранных услуг"
        val x10011 = "-1"
        val x11841 = preferenceStorage.phone
        val x12440 = "Приложение"
        val x10941 = 10581
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
            ACTION1
        )
    }

    private fun issueGoToTheOfficeMyV2(address: String, connectedServicesText: String) {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.CONNECT_SERVICES_HAS_COMMON,
            userName = preferenceStorage.sentName.toString(),
            inputAddress = address,
            services = connectedServicesText
        )
        super.createIssueV2(issue)
    }
}
