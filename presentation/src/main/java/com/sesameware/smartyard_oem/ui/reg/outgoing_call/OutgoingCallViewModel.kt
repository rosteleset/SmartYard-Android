package com.sesameware.smartyard_oem.ui.reg.outgoing_call

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.response.UserName
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.checkAndRegisterPushToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OutgoingCallViewModel(
    override val mAuthInteractor: AuthInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    val phoneConfirmed = MutableLiveData(false)

    fun startRepeatingCheckPhone(deviceToken: String, userPhone: String, context: Context): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            var isDone = false
            while (isActive && !isDone) {
                try {
                    val res = mAuthInteractor.checkPhone(userPhone, deviceToken)
                    isDone = true
                    mPreferenceStorage.authToken = res.data.accessToken
                    val userName: UserName = if (res.data.names is Boolean)
                        UserName("", "")
                    else
                        Gson().fromJson(Gson().toJson(res.data.names), UserName::class.java)
                    mPreferenceStorage.userName = userName

                    //получение настроек
                    mAuthInteractor.getOptions()?.let { result ->
                        DataModule.providerConfig = result.data
                    }

                    phoneConfirmed.postValue(true)

                    break
                } catch (e: Throwable) {
                    //для теста
                    delay(CHECK_PHONE_DELAY)
                }
            }

            if (isDone) {
                checkAndRegisterPushToken(context.applicationContext)
            }
        }
    }

    companion object {
        const val CHECK_PHONE_DELAY = 5000L
    }
}
