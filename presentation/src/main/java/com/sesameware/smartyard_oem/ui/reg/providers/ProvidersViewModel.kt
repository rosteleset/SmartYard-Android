package com.sesameware.smartyard_oem.ui.reg.providers

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.response.Provider
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.tel.NumberRegFragment

class ProvidersViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor
) : GenericViewModel() {
    val providersList = MutableLiveData<List<Provider>?>(null)

    init {
        getProviders()
    }

    fun goToNext(fragment: Fragment, providerId: String, providerName: String, providerBaseUrl: String) {
        DataModule.BASE_URL = providerBaseUrl + if (!providerBaseUrl.endsWith("/")) "/" else ""
        DataModule.providerName = providerName
        mPreferenceStorage.providerId = providerId
        NavHostFragment.findNavController(fragment).navigate(R.id.action_providersFragment_to_numberRegFragment)
    }

    private fun getProviders() {
        viewModelScope.withProgress {
            val res = authInteractor.providers()
            providersList.postValue(res?.data)
        }
    }
}
