package com.sesameware.smartyard_oem.ui.reg.outgoing_call

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.sesameware.data.DataModule
import kotlinx.coroutines.*
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.response.Name
import com.sesameware.smartyard_oem.GenericViewModel

class OutgoingCallViewModel(
    private val authInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    val phoneConfirmed = MutableLiveData(Pair(false, Name("", "")))

    fun startRepeatingCheckPhone(userPhone: String): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            var isDone = false
            while (isActive && !isDone) {
                try {
                    val res = authInteractor.checkPhone(userPhone)
                    isDone = true
                    mPreferenceStorage.authToken = res.data.accessToken
                    val name: Name = if (res.data.names is Boolean)
                        Name("", "")
                    else
                        Gson().fromJson(Gson().toJson(res.data.names), Name::class.java)

                    //получение настроек
                    authInteractor.getOptions()?.let { result ->
                        DataModule.providerConfig = result.data
                    }

                    phoneConfirmed.postValue(Pair(true, name))

                    break
                } catch (e: Throwable) {
                    //для теста
                    delay(CHECK_PHONE_DELAY)
                }
            }

            if (isDone) {
                checkAndRegisterFcmToken()
            }
        }
    }

    companion object {
        const val CHECK_PHONE_DELAY = 5000L
    }
}
