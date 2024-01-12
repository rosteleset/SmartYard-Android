package com.sesameware.smartyard_oem.ui.main.address.addressVerification.courier

import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION2
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.IssueTypeV2
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 02/04/2020.
 */
class CourierViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private var preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {
    fun createIssue(address: String) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            createIssueV1(address)
        } else {
            createIssueV2(address)
        }
    }

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
    private fun createIssueV1(address: String) {
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

    private fun createIssueV2(address: String) {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.REQUEST_QR_CODE_COURIER,
            userName = preferenceStorage.sentName.toString(),
            inputAddress = address
        )
        super.createIssueV2(issue)
    }
}
