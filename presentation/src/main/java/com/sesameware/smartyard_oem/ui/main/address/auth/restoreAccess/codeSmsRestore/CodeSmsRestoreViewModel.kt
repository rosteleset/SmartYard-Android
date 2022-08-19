package com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess.codeSmsRestore

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.model.CommonError

import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 22/03/2020.
 */
class CodeSmsRestoreViewModel(
    private val addressInteractor: AddressInteractor
) : GenericViewModel() {

    val navigationToDialog = MutableLiveData<Event<Unit>>()

    val time = MutableLiveData<String>()

    val confirmError = MutableLiveData<Event<CommonError>>()

    val sentCodeError = MutableLiveData<Event<CommonError>>()

    val resendTimerUp = MutableLiveData<Event<Unit>>()
    val resendTimerStarted = MutableLiveData<Event<Unit>>()

    private var countDownTimer = object : CountDownTimer(60000, 1000) {
        override fun onFinish() {
            resendTimerUp.value = Event(Unit)
        }

        override fun onTick(millisUntilFinished: Long) {
            time.value = convertSecondsToHMmSs(millisUntilFinished) ?: ""
        }
    }

    init {
        startResendTimer()
    }

    fun confirmCodeRecovery(contract: String, code: String) {
        viewModelScope.withProgress({
            confirmError.value = Event(it)
            it.httpCode != 403
        }) {
            addressInteractor.confirmCodeRecovery(contract, code)
            navigationToDialog.postValue(Event(Unit))
        }
    }

    private fun startResendTimer() {
        resendTimerStarted.value = Event(Unit)
        countDownTimer.start()
    }

    fun cancelTimer() {
        countDownTimer.cancel()
    }

    private fun convertSecondsToHMmSs(milliseconds: Long): String? {
        val seconds = (milliseconds / 1000).toInt() % 60
        val minutes = (milliseconds / (1000 * 60) % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun sentCodeRecovery(contract: String, contractId: String) {
        viewModelScope.withProgress({
            sentCodeError.value = Event(it)
            true
        }) {
            addressInteractor.sentCodeRecovery(contract, contractId)
            startResendTimer()
        }
    }
}
