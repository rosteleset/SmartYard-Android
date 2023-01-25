package com.sesameware.smartyard_oem.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.interactors.PayInteractor
import com.sesameware.smartyard_oem.App
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.SettingsViewModel
import timber.log.Timber

class MainActivityViewModel(
    private val authInteractor: AuthInteractor,
    private val mPreferenceStorage: PreferenceStorage,
    private val inboxInteractor: InboxInteractor,
    private val payInteractor: PayInteractor
) : GenericViewModel() {

    val bottomNavigateTo = MutableLiveData<Event<Int>>()
    val badge = MutableLiveData<Boolean>()
    val chat = MutableLiveData<Boolean>()
    val chatSendMsg = MutableLiveData<Event<String>>()
    val chatSendFileUri = MutableLiveData<Event<Uri>>()

    val chatOnReceiveFilePermission = MutableLiveData<Event<Boolean>>()

    private val _navigationToAddressAuthFragmentAction = MutableLiveData<Event<Unit>>()
    val navigationToAddressAuthFragmentAction: LiveData<Event<Unit>>
        get() = _navigationToAddressAuthFragmentAction

    private val _reloadToAddress = MutableLiveData<Event<Unit>>()
    val reloadToAddress: LiveData<Event<Unit>>
        get() = _reloadToAddress

    private val _updateToAppNavigateDialog = MutableLiveData<Event<Update>>()
    val updateToAppNavigateDialog: LiveData<Event<Update>>
        get() = _updateToAppNavigateDialog

    fun navigationToAddressAuthFragmentAction() {
        _navigationToAddressAuthFragmentAction.value = Event(Unit)
    }

    fun navigationToAddress() {
        _reloadToAddress.value = Event(Unit)
    }

    fun onCreate() {
        checkAndRegisterFcmToken()
    }

    fun onResume() {
        unread()
    }

    private fun unread() {
        viewModelScope.withProgress({ false }, null) {
            val res = inboxInteractor.unread()
            if (res.data.count <= 0)
                badge.postValue(false)
            else
                badge.postValue(true)
        }
    }

    fun badgeParse(count: Int) {
        if (count <= 0) badge.postValue(false) else badge.postValue(true)
    }

    fun navigateToChatAndMsg(context: Context, data: SettingsViewModel.DialogServiceData) {
        bottomNavigate(R.id.chat)
        val serviceName = context.getString(data.service.nameId)
        val noContract = context.getString(R.string.chat_no_contract)
        val contractName = if (data.contractName.isEmpty()) noContract else data.contractName
        var msg = context.getString(data.dialog.chatMsg, serviceName, contractName)
        if (BuildConfig.BUILD_TYPE != App.release) {
            msg += " ${context.getString(R.string.chat_test_msg)}"
        }
        callJsSendMessage(msg)
    }

    private fun bottomNavigate(@IdRes id: Int) {
        bottomNavigateTo.postValue(Event(id))
    }

    private fun callJsSendMessage(string: String) {
        chatSendMsg.postValue(Event(string))
    }

    fun appVersion(version: String) {
        viewModelScope.withProgress(progress = null) {
            val response = authInteractor.appVersion(version)
            _updateToAppNavigateDialog.value = Event(
                when (response?.data) {
                    "upgrade" -> Update.UPGRADE
                    "force_upgrade" -> Update.FORCE_UPGRADE
                    else -> Update.NONE
                }
            )
        }
    }

    enum class Update {
        NONE, UPGRADE, FORCE_UPGRADE
    }
}
