package ru.madbrains.smartyard.ui.reg

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.InboxInteractor
import ru.madbrains.smartyard.FirebaseMessagingService.TypeMessage
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.reg.tel.NumberRegFragmentDirections
import timber.log.Timber

class RegistrationViewModel(
    private val mPreferenceStorage: PreferenceStorage,
    private val inboxInteractor: InboxInteractor
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
                    .navigate(R.id.action_numberRegFragment_to_appealFragment)
            } else {
                val action =
                    NumberRegFragmentDirections.actionNumberRegFragmentToMainActivity(
                        messageId
                    )
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
}
