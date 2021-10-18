package ru.madbrains.smartyard.ui.main.pay.contract.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel

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
