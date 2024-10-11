package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.AddressRepository
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.response.AcceptOffertaResponse
import ru.madbrains.domain.model.response.GetAddressListResponse
import ru.madbrains.domain.model.response.GetSettingsListResponse
import ru.madbrains.domain.model.response.IntercomResponse
import ru.madbrains.domain.model.response.ResetCodeResponse
import ru.madbrains.domain.model.response.OfficesResponse
import ru.madbrains.domain.model.response.RecoveryOptionsResponse
import ru.madbrains.domain.model.response.SentCodeRecoveryResponse
import ru.madbrains.domain.model.response.ConfirmCodeRecoveryResponse
import ru.madbrains.domain.model.response.AddMyPhoneResponse
import ru.madbrains.domain.model.response.QRResponse
import ru.madbrains.domain.model.response.RoommateResponse
import ru.madbrains.domain.model.response.AccessResponse
import ru.madbrains.domain.model.response.ActivateLimitResponse
import ru.madbrains.domain.model.response.ResendResponse
import ru.madbrains.domain.model.response.PlogDaysResponse
import ru.madbrains.domain.model.response.PlogResponse
import ru.madbrains.domain.model.response.CamMapResponse
import ru.madbrains.domain.model.response.CameraCctvResponse
import ru.madbrains.domain.model.response.CheckOffertaResponse
import ru.madbrains.domain.model.response.ContractsResponse
import ru.madbrains.domain.model.response.LogOutResponse
import ru.madbrains.domain.model.response.OpenUrlResponse
import ru.madbrains.domain.model.response.ParentControlResponse
import ru.madbrains.domain.model.response.PlacesCctvResponse
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
class AddressInteractor(
    private val repository: AddressRepository
) {
    suspend fun generateOpenUrl(
        houseId: Int,
        flat: Int,
        domophoneId: Long,
        timeExpire: Int = 43200,
        count: Int = 1
    ): OpenUrlResponse {
        return repository.generateOpenUrl(houseId, flat, domophoneId, timeExpire, count)
    }

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

    suspend fun resetCode(flatId: Int, domophoneId: Long): ResetCodeResponse {
        return repository.resetCode(flatId, domophoneId)
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

    suspend fun checkOfferta(login: String, password: String): CheckOffertaResponse {
        return repository.checkOfferta(login, password)
    }

    suspend fun checkOffertaByAddress(houseId: Int, flat: Int): CheckOffertaResponse {
        return repository.checkOffertaByAddress(houseId, flat)
    }

    suspend fun acceptOfferta(login: String, password: String): AcceptOffertaResponse {
        return repository.acceptOfferta(login, password)
    }

    suspend fun acceptOffertaByAddress(houseId: Int, flatId: Int): AcceptOffertaResponse {
        return repository.acceptOffertaByAddress(houseId, flatId)
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

    suspend fun plogDays(flatId: Int, events: Set<Int>? = null): PlogDaysResponse {
        return repository.plogDays(flatId, events)
    }

    suspend fun plog(flatId: Int, day: String): PlogResponse {
        return repository.plog(flatId, day)
    }

    suspend fun camMap(): CamMapResponse {
        return repository.camMap()
    }

    suspend fun getPlaces(): PlacesCctvResponse {
        return repository.getPlaces()
    }

    suspend fun geCameras(): CameraCctvResponse {
        return repository.getCameras()
    }

    suspend fun getContracts(): ContractsResponse {
        return repository.getContracts()
    }

    suspend fun setParentControl(clientId: Int): ParentControlResponse {
        return repository.setParentControl(clientId)
    }

    suspend fun activateLimit(contractId: Int): ActivateLimitResponse {
        return repository.activateLimit(contractId)
    }
}
