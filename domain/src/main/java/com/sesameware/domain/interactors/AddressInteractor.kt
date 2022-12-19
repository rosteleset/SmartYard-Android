package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.AddressRepository
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.response.GetAddressListResponse
import com.sesameware.domain.model.response.GetSettingsListResponse
import com.sesameware.domain.model.response.IntercomResponse
import com.sesameware.domain.model.response.ResetCodeResponse
import com.sesameware.domain.model.response.OfficesResponse
import com.sesameware.domain.model.response.RecoveryOptionsResponse
import com.sesameware.domain.model.response.SentCodeRecoveryResponse
import com.sesameware.domain.model.response.ConfirmCodeRecoveryResponse
import com.sesameware.domain.model.response.AddMyPhoneResponse
import com.sesameware.domain.model.response.QRResponse
import com.sesameware.domain.model.response.RoommateResponse
import com.sesameware.domain.model.response.AccessResponse
import com.sesameware.domain.model.response.ResendResponse
import com.sesameware.domain.model.response.PlogDaysResponse
import com.sesameware.domain.model.response.PlogResponse
import com.sesameware.domain.model.response.CamMapResponse

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
class AddressInteractor(
    private val repository: AddressRepository
) {
    suspend fun getAddressList(): GetAddressListResponse {
        return repository.getAddressList()
    }

    suspend fun getSettingsList(): GetSettingsListResponse {
        return repository.getSettingsList()
    }

    suspend fun putIntercom(
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
    ): IntercomResponse {
        return repository.putIntercom(
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
    }

    suspend fun getIntercom(
        flatId: Int
    ): IntercomResponse {
        return repository.getIntercom(
            flatId
        )
    }

    suspend fun resetCode(flatId: Int): ResetCodeResponse {
        return repository.resetCode(flatId)
    }

    suspend fun getOffices(): OfficesResponse {
        return repository.getOffices()
    }

    suspend fun recoveryOptions(contract: String): RecoveryOptionsResponse {
        return repository.recoveryOptions(contract)
    }

    suspend fun sentCodeRecovery(
        contract: String,
        contractId: String
    ): SentCodeRecoveryResponse {
        return repository.sentCodeRecovery(
            contract,
            contractId
        )
    }

    suspend fun confirmCodeRecovery(
        contract: String,
        code: String
    ): ConfirmCodeRecoveryResponse {
        return repository.confirmCodeRecovery(
            contract,
            code
        )
    }

    suspend fun addMyPhone(
        login: String,
        password: String,
        comment: String?,
        notification: String?
    ): AddMyPhoneResponse {
        return repository.addMyPhone(login, password, comment, notification)
    }

    suspend fun registerQR(
        url: String
    ): QRResponse {
        return repository.registerQR(url)
    }

    suspend fun getRoommate(): RoommateResponse {
        return repository.getRoommate()
    }

    suspend fun access(
        flatId: Int,
        guestPhone: String?,
        type: String?,
        expire: String?,
        clientId: String?
    ): AccessResponse {
        return repository.access(flatId, guestPhone, type, expire, clientId)
    }

    suspend fun resend(flatId: Int, guestPhone: String): ResendResponse {
        return repository.resend(flatId, guestPhone)
    }

    suspend fun plogDays(flatId: Int, events: Set<Int>): PlogDaysResponse {
        return repository.plogDays(flatId, events)
    }

    suspend fun plog(flatId: Int, day: String): PlogResponse {
        return repository.plog(flatId, day)
    }

    suspend fun camMap(): CamMapResponse {
        return repository.camMap()
    }
}
