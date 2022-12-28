package com.sesameware.smartyard_oem.ui.reg

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.FirebaseMessagingService.TypeMessage
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.providers.ProvidersFragmentDirections
import timber.log.Timber

class RegistrationViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val inboxInteractor: InboxInteractor,
    private val authInteractor: AuthInteractor
) : GenericViewModel() {
    private val TAG = RegistrationViewModel::class.simpleName

    fun onStart(
        fragment: Fragment,
        messageId: String,
        messageType: TypeMessage,
        activity: Activity
    ) {
        if (mPreferenceStorage.authToken == null) {
            if (BuildConfig.PROVIDER_URL.isNotEmpty()) {
                val action = ProvidersFragmentDirections.actionGlobalNumberRegFragment()
                val options = NavOptions.Builder()
                    .setPopUpTo(R.id.providersFragment, true)
                    .build()
                NavHostFragment.findNavController(fragment).navigate(action, options)
            }
        } else {
            if (mPreferenceStorage.sentName == null) {
                NavHostFragment.findNavController(fragment)
                    .navigate(R.id.action_providersFragment_to_appealFragment)
            } else {
                val action =
                    ProvidersFragmentDirections.actionProvidersFragmentToMainActivity(messageId)
                action.messageType = messageType
                fragment.findNavController().navigate(action)
                activity.finish()
            }
        }
    }

    suspend fun getProviderConfig() {
        Timber.d("debug_dmm call getProviderConfig")
        if (BuildConfig.PROVIDER_URL.isNotEmpty()) {
            DataModule.BASE_URL = BuildConfig.PROVIDER_URL + if (!BuildConfig.PROVIDER_URL.endsWith("/")) "/" else ""
            mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
            Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
            DataModule.providerName = BuildConfig.PROVIDER_NAME
            authInteractor.getOptions()?.let { result ->
                DataModule.providerConfig = result.data
            }

            return
        } else {
            val pId = mPreferenceStorage.providerId
            if (pId?.isNotEmpty() == true) {
                authInteractor.providers()?.data?.forEach {
                    if (it.id == pId) {
                        DataModule.BASE_URL = it.baseUrl + if (!it.baseUrl.endsWith("/")) "/" else ""
                        mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
                        Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
                        DataModule.providerName = it.name
                        authInteractor.getOptions()?.let { result ->
                            DataModule.providerConfig = result.data
                        }

                        return
                    }
                }
            }
        }
    }
}
