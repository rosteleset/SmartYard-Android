package ru.madbrains.smartyard.ui.common

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.data.prefs.SentName
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.ui.reg.sms.SmsRegFragment

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class AppealFormViewModel(
    private val mInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    val sentName = MutableLiveData<SentName>()

    fun sendName(
        name: String,
        patronimic: String?,
        listenerEmpty: listenerEmpty
    ) {
        viewModelScope.withProgress({ false }) {
            mInteractor.sendName(name, patronimic)
            mPreferenceStorage.sentName = SentName(name, patronimic)
            listenerEmpty()
        }
    }

    fun loadName(bundle: Bundle?) {
        mPreferenceStorage.sentName?.let {
            sentName.postValue(it)
        } ?: run {
            bundle?.let {
                sentName.postValue(
                    SentName(
                        it.getString(SmsRegFragment.KEY_NAME) ?: "",
                        it.getString(SmsRegFragment.KEY_PATRONYMIC) ?: ""
                    )
                )
            }
        }
    }
}
