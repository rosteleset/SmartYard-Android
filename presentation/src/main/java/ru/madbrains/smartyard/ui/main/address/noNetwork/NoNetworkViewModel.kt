package ru.madbrains.smartyard.ui.main.address.noNetwork

import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.model.request.CreateIssuesRequest.CustomFields
import ru.madbrains.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import ru.madbrains.smartyard.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 02/04/2020.
 */
class NoNetworkViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    /**
     """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$\nТелефон: $телефон$\nАдрес, введённый пользователем: $адрес$.\nСписок подключаемых услуг $услуги$
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

    fun createIssue(address: String, services: List<String>) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}\n Телефон: ${preferenceStorage.phone}\n Адрес, введённый пользователем: $address\n Список подключаемых услуг: ${services.joinToString { it -> "\'${it}\'" }}"
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
