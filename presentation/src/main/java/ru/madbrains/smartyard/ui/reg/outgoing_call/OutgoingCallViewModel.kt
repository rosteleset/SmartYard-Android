package ru.madbrains.smartyard.ui.reg.outgoing_call

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.*
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.model.response.Name
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.p8

class OutgoingCallViewModel(
    private val authInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    val phoneConfirmed = MutableLiveData(Pair(false, Name("", "")))

    fun startRepeatingCheckPhone(fragment: Fragment, userPhone: String): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            var isDone = false
            while (isActive && !isDone) {
                try {
                    val res = authInteractor.checkPhone(userPhone.p8)
                    isDone = true
                    mPreferenceStorage.authToken = res.data.accessToken
                    val name: Name = if (res.data.names is Boolean)
                        Name("", "")
                    else
                        Gson().fromJson(Gson().toJson(res.data.names), Name::class.java)
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
