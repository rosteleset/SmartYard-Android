package com.sesameware.smartyard_oem.ui.main.settings.addressSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.request.CreateIssuesRequest.CustomFields
import com.sesameware.domain.model.request.CreateIssuesRequest.TypeAction.ACTION1
import com.sesameware.domain.model.response.Intercom
import com.sesameware.smartyard_oem.p8
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

/**
 * @author Nail Shakurov
 * Created on 13/03/2020.
 */
class AddressSettingsViewModel(
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor,
    private val addressInteractor: AddressInteractor,
    val preferenceStorage: PreferenceStorage
) : BaseIssueViewModel(geoInteractor, issueInteractor) {

    val intercom = MutableLiveData<Intercom>()
    val deleteRoommate = MutableLiveData<Unit>()

    fun getIntercom(flatId: Int) {
        viewModelScope.withProgress {
            preferenceStorage.xDmApiRefresh = true
            val res = addressInteractor.getIntercom(flatId)
            intercom.postValue(res.data)
        }
    }

    fun putIntercom(
        flatId: Int,
        enableDoorCode: TF?,
        cms: TF?,
        voip: TF?,
        autoOpen: String?,
        whiteRabbit: Int?,
        paperBill: TF?,
        disablePlog: TF?,
        hiddenPlog: TF?,
        frsDisabled: TF?
    ) {
        viewModelScope.withProgress {
            preferenceStorage.xDmApiRefresh = true
            val res = addressInteractor.putIntercom(
                flatId,
                enableDoorCode,
                cms,
                voip,
                autoOpen,
                whiteRabbit,
                paperBill,
                disablePlog,
                hiddenPlog,
                frsDisabled
            )
            intercom.postValue(res.data)
        }
    }

    fun deleteRoommate(flatId: Int, clientId: String) {
        viewModelScope.withProgress {
            addressInteractor.access(
                flatId,
                preferenceStorage.phone?.p8,
                null,
                "2001-01-01 00:00:00",
                clientId
            )
            deleteRoommate.postValue(Unit)
        }
    }

    fun saveSoundToPref(
        tone: SoundChooser.RingtoneU,
        flatId: Int
    ) {
        val cur = preferenceStorage.addressOptions
        cur.getOption(flatId).notifySoundUri = tone.uri.toString()
        preferenceStorage.addressOptions = cur
    }

    fun saveSpeakerFlag(flatId: Int, flag: Boolean) {
        val addressOpts = preferenceStorage.addressOptions
        addressOpts.getOption(flatId).isSpeaker = flag
        preferenceStorage.addressOptions = addressOpts
    }

    /**    """issue"": {
     ""project"": ""REM"",
     ""summary"": ""Авто: Заявка с сайта"",
     ""description"":ФИО: $как к вам обращаться$\nТелефон: $телефон$\nАдрес, введённый пользователем: $адрес$\nУдаление адреса из приложения. Причина $описание$
     ""type"": 32
     },
     ""customFields"": {
     ""10011"": ""-1"",
     ""11841"": $телефон, введенный пользователем$,
     ""12440"": ""Приложение"",
     ""10743"": $широта$,
     ""10744"": $долгота$,
     },
     ""actions"": [
     ""Начать работу"",
     ""Позвонить ""
     ]
     }"*/

    fun createIssue(address: String, reasonText: String, reasonList: String) {
        val summary = "Авто: Заявка с сайта"
        val description =
            "ФИО: ${preferenceStorage.sentName}\n Телефон: ${preferenceStorage.phone}\n Адрес, введённый пользователем: $address.\nУдаление адреса из приложения. Причина: $reasonText($reasonList)"
        val x10011 = "-1"
        val x11841 = preferenceStorage.phone
        val x12440 = "Приложение"
        super.createIssue(
            summary,
            description,
            address,
            CustomFields(
                x10011 = x10011,
                x11841 = x11841,
                x12440 = x12440
            ),
            ACTION1
        )
    }
}
