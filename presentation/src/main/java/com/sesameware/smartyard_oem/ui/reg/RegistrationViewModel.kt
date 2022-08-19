package com.sesameware.smartyard_oem.ui.reg

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
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
        if (mPreferenceStorage.authToken != null) {
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

    fun delivered(messageId: String) {
        viewModelScope.withProgress({ false }) {
            inboxInteractor.delivered(messageId)
            Timber.tag(TAG).d("delivered")
        }
    }

    suspend fun getProviderConfig() {
        val pId = mPreferenceStorage.providerId
        if (pId?.isNotEmpty() == true) {
            authInteractor.providers()?.data?.forEach {
                if (it.id == pId) {
                    DataModule.BASE_URL = it.baseUrl + if (!it.baseUrl.endsWith("/")) "/" else ""
                    authInteractor.getOptions()?.let { result ->
                        DataModule.providerConfig = result.data
                    }

                    return
                }
            }
        }
    }
}
