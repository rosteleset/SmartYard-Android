package com.sesameware.smartyard_oem

import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber


fun GenericViewModel.checkAndRegisterPushToken(applicationContext: Context) {
    val crashlytics = Crashlytics.getInstance()
    crashlytics.setUserId("_user_${mPreferenceStorage.phone.orEmpty()}")
    val deviceInfo = "Manufacturer: ${Build.MANUFACTURER}, model: ${Build.MODEL}, device: ${Build.DEVICE}, release: ${Build.VERSION.RELEASE}, SDK: ${Build.VERSION.SDK_INT}"
    crashlytics.setCustomKey("_device_info", deviceInfo)

    //для заданного пользователя не обновляем FCM токен
    //return

    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Timber.w("debug_dmm fetching fcm token failed")

            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val phone = mPreferenceStorage.phone
            val exceptionMessage = task.exception?.message
            Timber.w("debug_dmm exception message: $exceptionMessage")
            Timber.w("debug_dmm Device info: $deviceInfo")
            Crashlytics.getInstance().log("Date: $date; Phone: $phone; Device info: $deviceInfo; Message: $exceptionMessage\n")
            task.exception?.let { exception->
                Crashlytics.getInstance().recordException(exception)
            }
            return@addOnCompleteListener
        }
        mPreferenceStorage.pushToken = task.result

        //Timber.d("debug_dmm fcm token now: ${task.result?.token}")

        mPreferenceStorage.pushToken?.let { token ->
            Timber.d("debug_dmm token: $token")
            Timber.d("debug_dmm saved registered token: ${mPreferenceStorage.pushTokenRegistered}")
            if (token != mPreferenceStorage.pushTokenRegistered) {
                viewModelScope.launchSimple {
                    mAuthInteractor.registerPushToken(token)
                    mPreferenceStorage.pushTokenRegistered = token
                }
            }
        }
    }
}

fun GenericViewModel.refreshPushToken(applicationContext: Context) {
    Thread {
        Timber.d("debug_dmm refreshing fcm token..")
        FirebaseMessaging.getInstance().deleteToken()
        mPreferenceStorage.pushToken = ""
    }.start()
}
