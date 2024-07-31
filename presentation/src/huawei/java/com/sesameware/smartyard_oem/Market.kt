package com.sesameware.smartyard_oem

import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber

fun GenericViewModel.checkAndRegisterPushToken(applicationContext: Context) {
    val crashlytics = Crashlytics.getInstance()
    crashlytics.setUserId("_user_${mPreferenceStorage.phone.orEmpty()}")
    val deviceInfo = "Manufacturer: ${Build.MANUFACTURER}, model: ${Build.MODEL}, device: ${Build.DEVICE}, release: ${Build.VERSION.RELEASE}, SDK: ${Build.VERSION.SDK_INT}"
    crashlytics.setCustomKey("_device_info", deviceInfo)

    //для заданного пользователя не обновляем push токен
    //return

    try {
        Timber.d("debug_dmm    obtaining AAID")
        val aaid = HmsInstanceId.getInstance(applicationContext).id
        Timber.d("debug_dmm    AAID: $aaid")
    } catch (e: ApiException) {
        Timber.d("debug_dmm    obtaining AAID failed: $e")
    }

    Thread {
        try {
            val appId = AGConnectOptionsBuilder().build(applicationContext).getString("client/app_id")
            Timber.d("debug_dmm    obtaining HMS token for appId $appId")
            val token = HmsInstanceId.getInstance(applicationContext).getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)
            if (token.isNotEmpty()) {
                Timber.d("debug_dmm HMS token: $token")
                Timber.d("debug_dmm saved registered HMS token: ${mPreferenceStorage.pushTokenRegistered}")
                mPreferenceStorage.pushToken = token
                if (token != mPreferenceStorage.pushTokenRegistered) {
                    viewModelScope.launchSimple {
                        mAuthInteractor.registerPushToken(token)
                        mPreferenceStorage.pushTokenRegistered = token
                    }
                }
            }
        } catch (e: ApiException) {
            Timber.w("debug_dmm    obtaining token failed: $e")

            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val phone = mPreferenceStorage.phone
            val exceptionMessage = e.message
            Timber.w("debug_dmm exception message: $exceptionMessage")
            Timber.w("debug_dmm Device info: $deviceInfo")
            Crashlytics.getInstance().log("Date: $date; Phone: $phone; Device info: $deviceInfo; Message: $exceptionMessage\n")
            Crashlytics.getInstance().recordException(e)
        }
    }.start()
}

fun GenericViewModel.refreshPushToken(applicationContext: Context) {
    Thread {
        Timber.d("debug_dmm refreshing push token..")
        val appId = AGConnectOptionsBuilder().build(applicationContext).getString("client/app_id")
        HmsInstanceId.getInstance(applicationContext).deleteToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)
        mPreferenceStorage.pushToken = ""
    }.start()
}
