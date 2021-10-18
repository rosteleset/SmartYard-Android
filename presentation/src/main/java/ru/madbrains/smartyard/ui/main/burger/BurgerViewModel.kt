package ru.madbrains.smartyard.ui.main.burger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.interactors.SipInteractor
import ru.madbrains.domain.model.request.CreateIssuesRequest
import ru.madbrains.smartyard.ui.main.BaseIssueViewModel

class BurgerViewModel(
    private val sipInteractor: SipInteractor,
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    private val _dialNumber = MutableLiveData<String>()
    val dialNumber: LiveData<String>
        get() = _dialNumber

    var chosenSupportOption = MutableLiveData<SupportOption>()

    fun getHelpMe() {
        _dialNumber.value = ""
        viewModelScope.withProgress {
            sipInteractor.helpMe()?.let {
                _dialNumber.value = it.dial
            }
        }
    }

    fun createIssue() {
        val summary = "Авто: Звонок с приложения"
        val description = "Выполнить звонок клиенту по запросу с приложения"
        val x10011 = "-3"
        val x12440 = "Приложение"
        super.createIssue(
            summary,
            description,
            null,
            CreateIssuesRequest.CustomFields(x10011 = x10011, x12440 = x12440),
            CreateIssuesRequest.TypeAction.ACTION1
        )
    }

    enum class SupportOption {
        CALL_TO_SUPPORT_BY_PHONE, ORDER_CALLBACK
    }
}
