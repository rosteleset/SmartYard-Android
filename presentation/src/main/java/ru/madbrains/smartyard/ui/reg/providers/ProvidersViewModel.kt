package ru.madbrains.smartyard.ui.reg.providers

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import ru.madbrains.data.DataModule
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.model.response.Provider
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R

class ProvidersViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor
) : GenericViewModel() {
    val providersList = MutableLiveData<List<Provider>?>(null)

    init {
        getProviders()
    }

    fun goToNext(fragment: Fragment, providerId: String, providerBaseUrl: String) {
        DataModule.BASE_URL = providerBaseUrl + if (!providerBaseUrl.endsWith("/")) "/" else ""
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
