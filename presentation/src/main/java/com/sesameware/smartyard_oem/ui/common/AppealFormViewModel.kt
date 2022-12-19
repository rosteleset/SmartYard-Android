package com.sesameware.smartyard_oem.ui.common

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.data.prefs.SentName
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.reg.sms.SmsRegFragment

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
