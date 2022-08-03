package ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogDeleteReason

import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.model.request.CreateIssuesRequest.CustomFields
import ru.madbrains.domain.model.request.CreateIssuesRequest.TypeAction.ACTION2
import ru.madbrains.smartyard.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 03/04/2020.
 */
class DialogDeleteReasonViewModel(
    private val addressInteractor: AddressInteractor,
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    /**      """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$\nТелефон: $телефон$\nАдрес, введённый пользователем: $адрес$\nУдаление адреса из приложения. Причина $описание$
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
     ""Позвонить ""
     ]
     }"*/
    fun createIssue(address: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName} Телефон: ${preferenceStorage.phone} Адрес, введённый пользователем: $address nУдаление адреса из приложения. Причина описание\$"
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
