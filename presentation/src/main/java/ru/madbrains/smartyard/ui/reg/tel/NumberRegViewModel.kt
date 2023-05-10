package ru.madbrains.smartyard.ui.reg.tel

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.p8
import ru.madbrains.smartyard.ui.reg.sms.SmsRegFragment

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class NumberRegViewModel(
    private val mInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    fun requestSmsCode(phone: String, fragment: Fragment) {
        viewModelScope.withProgress({ false }) {
            mInteractor.requestCode(phone.p8)

            goToNext(phone, fragment)
        }
    }

    fun goToNext(phone: String, fragment: Fragment) {
        mPreferenceStorage.phone = phone
        NavHostFragment.findNavController(fragment)
            .navigate(
                R.id.action_numberRegFragment_to_smsRegFragment,
                bundleOf(
                    SmsRegFragment.KEY_PHONE_NUMBER to phone
                )
            )
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
