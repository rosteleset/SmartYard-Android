package ru.madbrains.smartyard.ui.reg.tel

import android.app.Activity
import android.os.Build
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.p8
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment
import ru.madbrains.smartyard.ui.reg.sms.SmsRegFragment
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class NumberRegViewModel(
    private val mInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    private var pushToken = ""


    fun requestSmsCode(phone: String, fragment: Fragment) {
        Timber.d("debug_dmm call checkAndRegisterFcmToken()")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w("debug_dmm fetching fcm token failed")
                val date =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val phone = mPreferenceStorage.phone
                val exceptionMessage = task.exception?.message
                val deviceInfo = "Manufacturer: " + Build.MANUFACTURER + ", model: " + Build.MODEL +
                        ", device: " + Build.DEVICE + ", release: " + Build.VERSION.RELEASE + ", SDK: " + Build.VERSION.SDK_INT
                Timber.w("debug_dmm exception message: $exceptionMessage")
                Timber.w("debug_dmm Device info: $deviceInfo")
                FirebaseCrashlytics.getInstance()
                    .log("Date: $date; Phone: $phone; Device info: $deviceInfo; Message: $exceptionMessage\n")
                task.exception?.let { exception ->
                    FirebaseCrashlytics.getInstance().recordException(exception)
                }
                return@addOnCompleteListener
            }
            mPreferenceStorage.fcmToken = task.result
            pushToken = mPreferenceStorage.fcmToken ?: ""
            requestAuthorization(phone, fragment)
        }
    }

    private fun requestAuthorization(phone: String, fragment: Fragment) {
        viewModelScope.withProgress({ false }) {
            val res = mInteractor.requestCodePush(phone.p8, "push", pushToken)
            if (res?.code == 200) {
                val requestId = res.data?.requestId?: ""
                goToPush(phone, fragment, requestId)
            }
            if ((res?.code == 204)) {
                goToNext(phone, fragment)
            }
        }
    }

    fun goToPush(phone: String, fragment: Fragment, requestId: String) {
        mPreferenceStorage.phone = phone
        NavHostFragment.findNavController(fragment)
            .navigate(
                R.id.action_numberRegFragment_to_pushRegFragment,
                bundleOf(
                    PushRegFragment.KEY_PHONE_NUMBER to phone,
                    PushRegFragment.REQUEST_ID to requestId,
                )
            )
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
