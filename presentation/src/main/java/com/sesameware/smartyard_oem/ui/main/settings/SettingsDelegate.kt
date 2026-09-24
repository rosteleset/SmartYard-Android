package com.sesameware.smartyard_oem.ui.main.settings

import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.smartyard_oem.databinding.FragmentSettingsBinding
import kotlinx.coroutines.CoroutineScope

interface SettingsDelegate {
    fun extendConfig(
        binding: FragmentSettingsBinding,
        adapter: SettingsAddressAdapter?
    )

    suspend fun refreshSentName(
        mAuthInteractor: AuthInteractor,
        mPreferenceStorage: PreferenceStorage
    ) = Unit
}
