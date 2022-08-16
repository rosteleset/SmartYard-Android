package ru.madbrains.smartyard.ui.reg.providers

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import ru.madbrains.data.DataModule
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.model.response.AuthType
import ru.madbrains.domain.model.response.Provider
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.reg.tel.NumberRegFragment

class ProvidersViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val authInteractor: AuthInteractor
) : GenericViewModel() {
    val providersList = MutableLiveData<List<Provider>?>(null)
    val authTypesList = MutableLiveData<List<AuthType>?>(null)

    init {
        getProviders()
    }

    fun goToNext(fragment: Fragment, providerId: String, providerBaseUrl: String) {
        DataModule.BASE_URL = providerBaseUrl + if (!providerBaseUrl.endsWith("/")) "/" else ""
        mPreferenceStorage.providerId = providerId

        viewModelScope.launchSimple {
            val res = authInteractor.authTypes()
            res?.data?.let { authList ->
                if (authList.size == 1) {
                    NavHostFragment.findNavController(fragment).navigate(
                        R.id.action_providersFragment_to_numberRegFragment,
                        bundleOf(NumberRegFragment.KEY_AUTH_METHOD_ID to authList[0])
                    )
                } else {
                    authTypesList.postValue(authList)
                    NavHostFragment.findNavController(fragment).navigate(R.id.action_providersFragment_to_authTypesFragment)
                }
            }
        }
    }

    private fun getProviders() {
        viewModelScope.launchSimple {
            val res = authInteractor.providers()
            providersList.postValue(res?.data)
        }
    }
}
