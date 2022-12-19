package com.sesameware.smartyard_oem.ui.main.pay.contract.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sesameware.domain.interactors.PayInteractor
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 05.06.2020.
 */
class PayWebViewViewModel(
    private val payInteractor: PayInteractor
) : GenericViewModel() {

    private val _navigateToSuccess = MutableLiveData<Event<String>>()
    val navigateToSuccess: LiveData<Event<String>>
        get() = _navigateToSuccess
}
