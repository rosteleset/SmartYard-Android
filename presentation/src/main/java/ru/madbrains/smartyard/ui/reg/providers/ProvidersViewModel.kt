package ru.madbrains.smartyard.ui.reg.providers

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.model.response.Provider
import ru.madbrains.domain.model.response.ProvidersListResponse
import ru.madbrains.smartyard.FirebaseMessagingService
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

    fun goToNext(activity: Activity, fragment: Fragment) {
        //NavHostFragment.findNavController(fragment).navigate(R.id.action_providersFragment_to_numberRegFragment)
        val action =
            ProvidersFragmentDirections.actionProvidersFragmentToMainActivity(
                ""
            )
            
        action.messageType = FirebaseMessagingService.TypeMessage.NO_DEFINE
        fragment.findNavController().navigate(action)
        activity.finish()
    }

    private fun getProviders() {
        viewModelScope.launchSimple {
            val res = authInteractor.providers()
            providersList.postValue(res?.data)
        }
    }
}
