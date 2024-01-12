package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogDeleteReason

import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION2
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.IssueTypeV2
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

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
    fun createIssue(address: String) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            createIssueV1(address)
        } else {
            createIssueV2(address)
        }
    }

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
    private fun createIssueV1(address: String) {
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

    private fun createIssueV2(address: String) {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.REMOVE_ADDRESS,
            inputAddress = address,
            userName = preferenceStorage.sentName.toString(),
            comments = ""
        )
        super.createIssueV2(issue)
    }
}
