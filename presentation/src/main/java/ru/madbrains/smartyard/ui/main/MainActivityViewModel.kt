package ru.madbrains.smartyard.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.DataModule
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.InboxInteractor
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.smartyard.App
import ru.madbrains.smartyard.BuildConfig
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.settings.SettingsViewModel
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
    val paySendIntent = MutableLiveData<Event<SendDataPay?>>()
    val sberPayIntent = MutableLiveData<Event<SendSberPay?>>()

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

    data class SendDataPay(var resultCode: Int, var data: Intent?)
    data class SendSberPay(var orderNumber: String?)

    fun navigationToAddressAuthFragmentAction() {
        _navigationToAddressAuthFragmentAction.value = Event(Unit)
    }

    fun navigationToAddress() {
        _reloadToAddress.value = Event(Unit)
    }

    fun onCreate() {
        Timber.d("debug_dmm baseUrl from storage: ${mPreferenceStorage.baseUrl}")
        DataModule.URL = mPreferenceStorage.baseUrl ?: DataModule.URL
        checkAndRegisterFcmToken()
    }

    fun onResume() {
        unread()
        sberCheckPayments()
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

    fun sberCompletePayment(orderNumber: String) {
        Timber.d("sberCompletePayment $orderNumber")
        viewModelScope.withProgress({false}, null) {
            val orderId = DataModule.extractOrderId(orderNumber)
            if (orderId.isNotEmpty()) {
                payInteractor.payProcess(orderNumber, orderId)
            }
        }
    }

    //Проверям статусы оплат. Если оплата успешна завершена, то дёргаем api метод.
    fun sberCheckPayments() {
        Timber.d("sberCheck")
        viewModelScope.withProgress({false}, null) {
            var orders = hashMapOf<String, String>()
            synchronized(DataModule.orderNumberToId) {
                orders = HashMap(DataModule.orderNumberToId)
            }
            val removeOrders = mutableListOf<String>()
            orders.forEach {
                val res = payInteractor.sberOrderStatusDo(DataModule.sberApiUserName,
                    DataModule.sberApiPassword, it.key)
                res?.actionCode?.let { actionCode ->
                    if (actionCode == 0) {
                        Timber.d("payProccess")
                        //оплата успешно выполнена, дёргаем api метод
                        payInteractor.payProcess(it.key, it.value)

                        //добавляем идентификатор платежа в список для удаления
                        removeOrders.add(it.key)
                    } else if (actionCode < 0) {
                        //ошибка при оплате, добавляем в список для удаления
                        removeOrders.add(it.key)
                    }
                }
            }

            //удаляем обработанные платежи
            Timber.d("__sber payments to remove: $removeOrders")
            synchronized(DataModule.orderNumberToId) {
                removeOrders.forEach {
                    DataModule.orderNumberToId.remove(it)
                }
            }
        }
    }

    enum class Update {
        NONE, UPGRADE, FORCE_UPGRADE
    }
}
