package com.sesameware.smartyard_oem.ui.reg.providers

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.domain.model.response.Provider
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class ProvidersViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor
) : GenericViewModel() {
    val providersList = MutableLiveData<List<Provider>?>(null)

    init {
        if (DataModule.BASE_URL.isEmpty()) {
            getProviders()
        }
    }

    fun goToNext(fragment: Fragment, providerId: String, providerName: String, providerBaseUrl: String) {
        DataModule.BASE_URL = providerBaseUrl + if (!providerBaseUrl.endsWith("/")) "/" else ""
        mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
        Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
        DataModule.providerName = providerName
        mPreferenceStorage.providerId = providerId
        DataModule.phonePattern = DataModule.defaultPhonePattern
        try {
            runBlocking {
                authInteractor.phonePattern()?.let { result ->
                    DataModule.phonePattern = result.data
                }
            }
        } catch(e: CommonErrorThrowable) {
            Timber.d("debug_dmm    phonePattern error: ${e.message}")
        }
        NavHostFragment.findNavController(fragment).navigate(R.id.action_providersFragment_to_numberRegFragment)
    }

    private fun getProviders() {
        viewModelScope.withProgress {
            val res = authInteractor.providers()
            providersList.postValue(res?.data)
        }
    }
}
