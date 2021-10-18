package ru.madbrains.smartyard.ui.main.settings.basicSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.data.prefs.SentName
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.model.TF
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.ui.SoundChooser

/**
 * @author Nail Shakurov
 * Created on 16/03/2020.
 */
class BasicSettingsViewModel(
    val preferenceStorage: PreferenceStorage,
    val databaseInteractor: DatabaseInteractor,
    val authInteractor: AuthInteractor
) : GenericViewModel() {

    val sentName = MutableLiveData<SentName>()

    val isPushSetting = MutableLiveData<Boolean>()
    val isPushMoneySetting = MutableLiveData<Boolean>()

    init {
        refreshSendName()
        viewModelScope.withProgress {
            val res = authInteractor.userNotification(
                null, null
            )
            isPushSetting.value = TF.getString(res.data.enable)
            isPushMoneySetting.value = TF.getString(res.data.money)
        }
    }

    fun setPushMoneySetting(flag: Boolean) {
        viewModelScope.withProgress {
            val res = authInteractor.userNotification(
                TF.getBoolean(flag),
                null
            )
            isPushMoneySetting.value = TF.getString(res.data.money)
        }
    }

    fun setPushSetting(flag: Boolean) {
        viewModelScope.withProgress {
            val res = authInteractor.userNotification(
                null,
                TF.getBoolean(flag)
            )
            isPushSetting.value = TF.getString(res.data.enable)
        }
    }

    fun refreshSendName() {
        sentName.postValue(preferenceStorage.sentName ?: SentName("", ""))
    }

    fun saveSoundToPref(tone: SoundChooser.RingtoneU) {
        preferenceStorage.notifySoundUri = tone.uri.toString()
    }
}
