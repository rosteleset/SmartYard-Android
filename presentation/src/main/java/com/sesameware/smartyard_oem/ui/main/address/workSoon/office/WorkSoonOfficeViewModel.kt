package com.sesameware.smartyard_oem.ui.main.address.workSoon.office

import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.IssueClass
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 03/04/2020.
 */
class WorkSoonOfficeViewModel(
    geoInteractor: GeoInteractor,
    private val issueInteractor: IssueInteractor,
    private val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

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
    fun createIssue(address: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}\n Адрес, введённый пользователем: $address.\n Требуется подтверждение адреса и подключение выбранных услуг"
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
            CreateIssuesRequest.TypeAction.ACTION1,
            IssueClass.ComeInOfficeMyselfIssue
        )
    }
}
