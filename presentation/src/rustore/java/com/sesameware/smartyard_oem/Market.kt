package com.sesameware.smartyard_oem

import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import ru.ok.tracer.crash.report.TracerCrashReport
import ru.rustore.sdk.pushclient.RuStorePushClient
import timber.log.Timber

fun GenericViewModel.checkAndRegisterPushToken(context: Context) {
    RuStorePushClient.getToken()
        .addOnSuccessListener {token ->
            if (token.isNotEmpty()) {
                Timber.d("debug_dmm RuStore token: $token")
                Timber.d("debug_dmm saved registered RuStore token: ${mPreferenceStorage.pushTokenRegistered}")
                mPreferenceStorage.pushToken = token
                if (token != mPreferenceStorage.pushTokenRegistered) {
                    viewModelScope.launchSimple({false}) {
                        mAuthInteractor.registerPushToken(token)
                        mPreferenceStorage.pushTokenRegistered = token
                    }
                }
            }
        }
        .addOnFailureListener {e ->
            Timber.w("debug_dmm    obtaining token failed: $e")

            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val phone = mPreferenceStorage.phone
            val exceptionMessage = e.message
            val deviceInfo = "Manufacturer: ${Build.MANUFACTURER}, model: ${Build.MODEL}, device: ${Build.DEVICE}, release: ${Build.VERSION.RELEASE}, SDK: ${Build.VERSION.SDK_INT}"
            Timber.w("debug_dmm exception message: $exceptionMessage")
            Timber.w("debug_dmm Device info: $deviceInfo")
            TracerCrashReport.log(phone ?: "")
            TracerCrashReport.log(deviceInfo)
            TracerCrashReport.report(e)
        }
}

fun GenericViewModel.refreshPushToken(context: Context) {
}
