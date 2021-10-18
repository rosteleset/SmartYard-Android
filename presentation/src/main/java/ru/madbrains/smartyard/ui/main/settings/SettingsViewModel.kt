package ru.madbrains.smartyard.ui.main.settings

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.data.prefs.SentName
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.model.Services
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.R
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 10/03/2020.
 */
class SettingsViewModel(
    private val addressInteractor: AddressInteractor,
    private val authInteractor: AuthInteractor,
    private val preferenceStorage: PreferenceStorage

) : GenericViewModel() {

    val dataList = MutableLiveData<List<SettingsAddressModel>>()

    val dialogService = MutableLiveData<Event<DialogServiceData>>()

    val progress = MutableLiveData<Boolean>()

    val sentName = MutableLiveData<SentName>()

    val phone = MutableLiveData<String>()

    //учитывать ли кэш при следующем запросе списка адресов для настройки
    var nextListNoCache = true

    enum class TypeDialog(
        @StringRes val title: Int,
        @StringRes val caption: Int,
        @StringRes val button: Int,
        @StringRes val chatMsg: Int
    ) {
        Connected(
            R.string.setting_dialog_service_connected_title,
            R.string.setting_dialog_service_connected_caption,
            R.string.setting_dialog_service_connected_button,
            R.string.setting_dialog_service_connected_chat
        ),
        NotConnected(
            R.string.setting_dialog_service_not_connected_title,
            R.string.setting_dialog_service_not_connected_caption,
            R.string.setting_dialog_service_not_connected_button,
            R.string.setting_dialog_service_not_connected_chat
        ),
        NotAvailable(
            R.string.setting_dialog_service_unavailable_title,
            R.string.setting_dialog_service_unavailable_caption,
            R.string.setting_dialog_service_unavailable_button,
            R.string.setting_dialog_service_unavailable_chat
        ),
        DomophoneConnected(
            R.string.setting_dialog_service_domophone_connected_title,
            R.string.setting_dialog_service_domophone_connected_caption,
            R.string.setting_dialog_service_domophone_connected_button,
            R.string.setting_dialog_service_domophone_connected_chat
        ),
        CCTVConnected(
            R.string.setting_dialog_service_cctv_connected_title,
            R.string.setting_dialog_service_cctv_connected_caption,
            R.string.setting_dialog_service_cctv_connected_button,
            R.string.setting_dialog_service_cctv_connected_chat
        )
    }

    data class DialogServiceData(
        val dialog: TypeDialog,
        val service: Services,
        val contractName: String
    )

    init {
        getDataList()
    }

    fun onStart() {
        refreshSentName()
    }

    fun refreshSentName() {
        sentName.postValue(
            preferenceStorage.sentName ?: SentName("", "")
        )
        phone.postValue(preferenceStorage.phone ?: "")
    }

    fun getDataList(forceRefresh: Boolean = false) {
        Timber.d("__Q__ call getDataList,  cache = $nextListNoCache")
        val noCache = nextListNoCache || forceRefresh
        nextListNoCache = false
        viewModelScope.withProgress(progress = progress) {
            if (noCache) {
                preferenceStorage.xDmApiRefresh = true
            }
            val res = addressInteractor.getSettingsList()
            dataList.postValue(
                res?.data?.map { settingItem ->
                    SettingsAddressModel(
                        settingItem.address,
                        settingItem.contractName,
                        settingItem.houseId,
                        settingItem.flatId,
                        settingItem.clientId,
                        settingItem.contractOwner.contains("t"),
                        settingItem.flatOwner,
                        settingItem.services,
                        settingItem.lcab,
                        settingItem.hasGates.contains("t")
                    )
                }
            )
        }
    }

    fun getAccess(
        service: Services,
        model: SettingsAddressModel,
        isConnected: Boolean
    ) {
        viewModelScope.withProgress {
            val resSer = authInteractor.getServices(model.houseId)
            val isAvailable = resSer?.data?.firstOrNull { it.icon == service.value } != null
            val type = if (isConnected) {
                when (service) {
                    Services.Domophone -> TypeDialog.DomophoneConnected
                    Services.Cctv -> TypeDialog.CCTVConnected
                    else -> TypeDialog.Connected
                }
            } else {
                if (isAvailable) TypeDialog.NotConnected else TypeDialog.NotAvailable
            }
            dialogService.postValue(
                Event(DialogServiceData(type, service, model.contractName))
            )
        }
    }
}
