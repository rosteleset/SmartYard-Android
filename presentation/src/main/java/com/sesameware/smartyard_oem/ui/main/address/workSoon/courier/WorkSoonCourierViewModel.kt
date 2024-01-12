package com.sesameware.smartyard_oem.ui.main.address.workSoon.courier

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
 * Created on 03/04/2020.
 */
class WorkSoonCourierViewModel(
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

    /** Для смены способа доставки надо кинуть 2 запроса.
     Запрос api/issues/action:
     {"customFields":[{"number":"10941","value":"Курьер"}],"key":"REM-418379","action":"Jelly.Способ доставки"}
     number - фиксированное поле
     value - новый тип доставки. Доступно два значения: "Курьер" и "Самовывоз"
     key - номер заявки
     action - фиксированное поле
     2. После успешного выполнения первого запроса кидаем еще один /api/issues/comment:
     {"key":"REM-418379","comment":"Cменился способ доставки. Подготовить пакет для курьера."}
     key - номер заявки
     comment - коммент для оператора. Доступно два значения, в зависимости от нового способа доставки:
     "Cменился способ доставки. Клиент подойдет в офис." и "Cменился способ доставки. Подготовить пакет для курьера."*/

    private fun createIssueV1(address: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}\n Адрес, введённый пользователем: $address.\n   клиент подойдет в офис для получения подтверждения."
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
            ACTION2
        )
    }

    private fun createIssueV2(address: String) {
        val issue = CreateIssuesRequestV2(
            type = IssueTypeV2.REQUEST_QR_CODE_OFFICE,
            userName = preferenceStorage.sentName.toString(),
            inputAddress = address,
            services = ""
        )
        super.createIssueV2(issue)
    }
}
