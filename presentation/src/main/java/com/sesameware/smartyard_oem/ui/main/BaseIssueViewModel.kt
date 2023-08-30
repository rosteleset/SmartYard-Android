package com.sesameware.smartyard_oem.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.IssueClass
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.CreateIssuesRequest.*
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

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
        typeAction: TypeAction,
        cls: IssueClass
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
                ).actions(typeAction.list
                ).cls(cls)
                    .build()
            )
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
            if (address != null) {
                _navigateToIssueFragmentAction.value = Event(IssueModel(address, result.data))
            }
        }
    }

    fun deleteIssue(key: String) {
        viewModelScope.withProgress {
            issueInteractor.actionIssue(key)
            _successNavigateToFragment.value = Event(Unit)
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

    fun changeDelivery(
        comment: String,
        key: String,
        value: String
    ) {
        viewModelScope.withProgress({
            true
        }) {
            issueInteractor.deliveryChange(value, key)
            issueInteractor.comment(comment, key)
            _navigateToIssueSuccessDialogAction.value = Event(Unit)
        }
    }
}
