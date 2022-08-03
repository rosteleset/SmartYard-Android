package ru.madbrains.smartyard.ui.main.address.addressVerification.courier

import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.model.request.CreateIssuesRequest.CustomFields
import ru.madbrains.domain.model.request.CreateIssuesRequest.TypeAction.ACTION2
import ru.madbrains.smartyard.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 02/04/2020.
 */
class CourierViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private var preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    /**
     """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$. Адрес, введённый пользователем: $адрес$. Подготовить конверт с qr-кодом. Далее заявку отправить курьеру.
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
     ""Передать в офис""
     ]
     }"*/
    fun createIssue(address: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName} Адрес, введённый пользователем: $address.\n  Подготовить конверт с qr-кодом. Далее заявку отправить курьеру."
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
            ACTION2
        )
    }
}
