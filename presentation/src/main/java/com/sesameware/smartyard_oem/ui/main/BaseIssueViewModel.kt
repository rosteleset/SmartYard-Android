package com.sesameware.smartyard_oem.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.ActionIssueRequestV2
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.Issue
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.DELIVERY_COURIER
import com.sesameware.domain.model.request.DELIVERY_OFFICE
import com.sesameware.domain.model.request.ISSUE_ACTION_CHANGE_QR_DELIVERY_TYPE
import com.sesameware.domain.model.request.ISSUE_ACTION_CLOSE
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel

/**
 * @author Nail Shakurov
 * Created on 24/04/2020.
 */
abstract class BaseIssueViewModel(
    private val geoInteractor: GeoInteractor,
    private val issueInteractor: IssueInteractor
) : GenericViewModel() {

    private val _navigateToIssueSuccessDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToIssueSuccessDialogAction: LiveData<Event<Unit>>
        get() = _navigateToIssueSuccessDialogAction

    private val _successNavigateToFragment = MutableLiveData<Event<Unit>>()
    val successNavigateToFragment: LiveData<Event<Unit>>
        get() = _successNavigateToFragment

    private val _navigateToIssueFragmentAction = MutableLiveData<Event<IssueModel>>()
    val navigateToIssueFragmentAction: LiveData<Event<IssueModel>>
        get() = _navigateToIssueFragmentAction

    fun createIssue(
        summary: String,
        description: String,
        address: String?,
        customFields: CustomFields,
        typeAction: TypeAction
    ) {
        val project = "REM"
        val type = 32L
        viewModelScope.withProgress({
            true
        }) {
            if (address != null) {
                val apiResult = geoInteractor.getCoder(address)
                customFields.x10743 = apiResult.data.lat.replace(".", ",")
                customFields.x10744 = apiResult.data.lon.replace(".", ",")
            }
            customFields.x11840 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"))
            val result = issueInteractor.createIssues(
                CreateIssuesRequest.Builder().issue(
                    Issue(
                        description,
                        project,
                        summary,
                        type = type
                    )
                ).customFields(
                    customFields
                ).actions(typeAction.list)
                    .build()
            )
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
            if (address != null) {
                _navigateToIssueFragmentAction.value = Event(IssueModel(address, result.data))
            }
        }
    }

    fun deleteIssue(key: String) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            deleteIssueV1(key)
        } else {
            deleteIssueV2(key)
        }
    }

    private fun deleteIssueV1(key: String) {
        viewModelScope.withProgress {
            issueInteractor.actionIssue(key)
            _successNavigateToFragment.value = Event(Unit)
        }
    }

    fun changeDelivery(
        comment: String,
        key: String,
        value: String
    ) {
        if (DataModule.providerConfig.issuesVersion != "2") {
            changeDeliveryV1(comment, key, value)
        } else {
            changeDeliveryV2(key, value)
        }
    }

    /** Для смены способа доставки надо кинуть 2 запроса.
     Запрос api/issues/action:
     {"customFields":[{"number":"10941","value":"Курьер"}],"key":"REM-418379","action":"Jelly.Способ доставки"}
     number - фиксированное поле
     value - новый тип доставки. Доступно два значения: "Курьер" и "Самовывоз"
     key - номер заявки
     action - фиксированное поле2.

     После успешного выполнения первого запроса кидаем еще один /api/issues/comment:
     {"key":"REM-418379","comment":"Cменился способ доставки. Подготовить пакет для курьера."}
     key - номер заявки
     comment - коммент для оператора. Доступно два значения, в зависимости от нового способа доставки:
     "Cменился способ доставки. Клиент подойдет в офис." и "Cменился способ доставки. Подготовить пакет для курьера."*/
    private fun changeDeliveryV1(
        comment: String,
        key: String,
        value: String
    ) {
        viewModelScope.withProgress({
            true
        }) {
            var deliveryType = value
            if (deliveryType == DELIVERY_COURIER) {
                deliveryType = "Курьер"
            }
            if (deliveryType == DELIVERY_OFFICE) {
                deliveryType = "Самовывоз"
            }
            issueInteractor.deliveryChange(deliveryType, key)
            issueInteractor.comment(comment, key)
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
        }
    }

    fun createIssueV2(issue: CreateIssuesRequestV2) {
        viewModelScope.withProgress({
            true
        }) {
            val result = issueInteractor.createIssueV2(issue)
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
            if (issue.inputAddress != null) {
                _navigateToIssueFragmentAction.value = Event(IssueModel(issue.inputAddress!!, result.data))
            }
        }
    }

    private fun deleteIssueV2(key: String) {
        viewModelScope.withProgress {
            val request = ActionIssueRequestV2(key, ISSUE_ACTION_CLOSE)
            issueInteractor.actionIssueV2(request)
            _successNavigateToFragment.value = Event(Unit)
        }
    }

    private fun changeDeliveryV2(
        key: String,
        value: String
    ) {
        viewModelScope.withProgress({
            true
        }) {
            val request = ActionIssueRequestV2(key, ISSUE_ACTION_CHANGE_QR_DELIVERY_TYPE, value)
            issueInteractor.actionIssueV2(request)
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
        }
    }
}
