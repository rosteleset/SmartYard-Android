package com.sesameware.smartyard_oem.ui.reg.tel

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.response.AuthMethod
import com.sesameware.domain.model.response.RequestCode
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.outgoing_call.OutgoingCallFragment
import com.sesameware.smartyard_oem.ui.reg.sms.SmsRegFragment
import kotlin.random.Random

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class NumberRegViewModel(
    private val mInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    private var authMethod = AuthMethod.SMS_CODE
    private var callNumber = ""

    fun requestSmsCode(phone: String, fragment: Fragment) {
        viewModelScope.withProgress({ false }) {
            val res = mInteractor.requestCode(phone)
            authMethod = res?.data?.method ?: AuthMethod.SMS_CODE
            res?.data?.confirmationNumbers?.let {
                callNumber = it[Random.nextInt(0, it.size)]
            }
            goToNext(phone, fragment)
        }
    }

    fun goToNext(phone: String, fragment: Fragment) {
        mPreferenceStorage.phone = phone
        when (authMethod) {
            AuthMethod.OUTGOING_CALL -> {
                NavHostFragment.findNavController(fragment).navigate(
                    R.id.action_numberRegFragment_to_outgoingCallFragment,
                    bundleOf(
                        OutgoingCallFragment.KEY_PHONE_NUMBER to phone,
                        OutgoingCallFragment.KEY_CALL_NUMBER to callNumber)
                )
            }
            AuthMethod.FLASH_CALL -> {
                NavHostFragment.findNavController(fragment)
                    .navigate(
                        R.id.action_numberRegFragment_to_smsRegFragment,
                        bundleOf(
                            SmsRegFragment.KEY_PHONE_NUMBER to phone,
                            SmsRegFragment.KEY_AUTH_METHOD to RequestCode.AUTH_METHOD_FLASH_CALL
                        )
                    )
            }
            else -> {
                NavHostFragment.findNavController(fragment)
                    .navigate(
                        R.id.action_numberRegFragment_to_smsRegFragment,
                        bundleOf(
                            SmsRegFragment.KEY_PHONE_NUMBER to phone,
                            SmsRegFragment.KEY_AUTH_METHOD to RequestCode.AUTH_METHOD_SMS_CODE
                        )
                    )
            }
        }
    }

    fun onStart(fragment: Fragment, activity: Activity) {
        if (mPreferenceStorage.authToken != null) {
            if (mPreferenceStorage.sentName == null) {
                NavHostFragment.findNavController(fragment)
                    .navigate(R.id.action_numberRegFragment_to_appealFragment)
            } else {
                NavHostFragment.findNavController(fragment)
                    .navigate(R.id.action_numberRegFragment_to_mainActivity)
                activity.finish()
            }
        }
    }
}
