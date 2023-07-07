package com.sesameware.smartyard_oem.ui.reg

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.FirebaseMessagingService.TypeMessage
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.providers.ProvidersFragmentDirections
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class RegistrationViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val inboxInteractor: InboxInteractor,
    val authInteractor: AuthInteractor
) : GenericViewModel() {
    fun onStart(
        fragment: Fragment,
        messageId: String,
        messageType: TypeMessage,
        activity: Activity
    ) {
        if (mPreferenceStorage.authToken == null) {
            if (DataModule.BASE_URL.isNotEmpty()) {
                mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
                Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
                try {
                    runBlocking {
                        authInteractor.phonePattern()?.let { result ->
                            DataModule.phonePattern = result.data
                        }
                    }
                } catch(e: CommonErrorThrowable) {
                    Timber.d("debug_dmm    phonePattern error: ${e.message}")
                }
                val action = ProvidersFragmentDirections.actionGlobalNumberRegFragment()
                val options = NavOptions.Builder()
                    .setPopUpTo(R.id.providersFragment, true)
                    .build()
                NavHostFragment.findNavController(fragment).navigate(action, options)
            }
        } else {
            runBlocking {
                try {
                    getProviderConfig()
                    authInteractor.phonePattern()?.let { result ->
                        DataModule.phonePattern = result.data
                    }
                } catch (e: CommonErrorThrowable) {
                    Timber.d("debug_dmm    getProviderConfig error: ${e.message}")
                    if (e.data.httpCode == 401) {
                        logout()
                    } else {

                    }
                }
            }

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
        if (DataModule.BASE_URL.isNotEmpty()) {
            mPreferenceStorage.providerBaseUrl = DataModule.BASE_URL
            Timber.d("debug_dmm    BASE_URL: ${DataModule.BASE_URL}")
            authInteractor.getOptions()?.let { result ->
                DataModule.providerConfig = result.data
            }

            return
        } else {
            val pId = mPreferenceStorage.providerId
            Timber.d("debug_dmm providerId = $pId")
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
