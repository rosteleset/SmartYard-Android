package com.sesameware.smartyard_oem.ui.common

import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.response.UserName
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 2020-02-05.
 */
class AppealFormViewModel(
    override val mAuthInteractor: AuthInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {
    
    val prefsUserName = mPreferenceStorage.userName

    fun sendName(
        name: String,
        patronymic: String,
        listenerEmpty: listenerEmpty
    ) {
        viewModelScope.withProgress({ false }) {
            mAuthInteractor.sendName(name, patronymic)
            mPreferenceStorage.userName = UserName(name, patronymic)
            listenerEmpty()
        }
    }
}
