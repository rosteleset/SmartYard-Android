package com.sesameware.smartyard_oem.ui.reg.sms

import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.CommonError
import com.sesameware.domain.model.response.UserName
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.checkAndRegisterPushToken

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class SmsRegViewModel(
    override val mAuthInteractor: AuthInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    val time = MutableLiveData<String>()

    val resendTimerUp = MutableLiveData<Event<Unit>>()
    val resendTimerStarted = MutableLiveData<Event<Unit>>()
    val confirmError = MutableLiveData<Event<CommonError>>()
    val sendPhoneError = MutableLiveData<Event<CommonError>>()

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

    fun confirmCode(deviceToken: String, phone: String, code: String, fragment: Fragment) {
        viewModelScope.withProgress({
            confirmError.value = Event(it)
            false
        }) {
            val res = mAuthInteractor.confirmCode(phone, code, deviceToken)
            mPreferenceStorage.authToken = res.data.accessToken

            //получение настроек
            mAuthInteractor.getOptions()?.let { result ->
                DataModule.providerConfig = result.data
            }

            mPreferenceStorage.userName = if (res.data.names is Boolean) {
                UserName()
            } else {
                Gson().fromJson<UserName>(Gson().toJson(res.data.names), UserName::class.java)
            }

            val action = SmsRegFragmentDirections.actionSmsRegFragmentToAppealFragment()
            fragment.findNavController().navigate(action)

            checkAndRegisterPushToken(fragment.requireContext().applicationContext)
        }
    }

    fun resendCode(phone: String, deviceToken: String) {
        viewModelScope.withProgress({
            sendPhoneError.value = Event(it)
            true
        }) {
            mAuthInteractor.requestCode(phone, deviceToken)
            startResendTimer()
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
}
