package ru.madbrains.domain.interfaces

import retrofit2.http.Field
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.response.AcceptOffertaResponse
import ru.madbrains.domain.model.response.GetAddressListResponse
import ru.madbrains.domain.model.response.GetSettingsListResponse
import ru.madbrains.domain.model.response.IntercomResponse
import ru.madbrains.domain.model.response.ResetCodeResponse
import ru.madbrains.domain.model.response.AddMyPhoneResponse
import ru.madbrains.domain.model.response.OfficesResponse
import ru.madbrains.domain.model.response.RecoveryOptionsResponse
import ru.madbrains.domain.model.response.ConfirmCodeRecoveryResponse
import ru.madbrains.domain.model.response.SentCodeRecoveryResponse
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

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
interface AddressRepository {
    suspend fun generateOpenUrl(
        houseId: Int,
        flat: Int,
        domophoneId: Long,
        timeExpire: Int,
        count: Int
    ): OpenUrlResponse

    suspend fun getAddressList(): GetAddressListResponse

    suspend fun getSettingsList(): GetSettingsListResponse

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
    ): IntercomResponse

    suspend fun getIntercom(
        flatId: Int
    ): IntercomResponse

    suspend fun resetCode(@Field("flatId") flatId: Int): ResetCodeResponse
    suspend fun resetCode(@Field("flatId") flatId: Int, @Field("domophoneId") domophoneId: Long): ResetCodeResponse

    suspend fun addMyPhone(
        login: String,
        password: String,
        comment: String?,
        notification: String?
    ): AddMyPhoneResponse

    suspend fun checkOfferta(login: String, password: String): CheckOffertaResponse

    suspend fun checkOffertaByAddress(houseId: Int, flatId: Int): CheckOffertaResponse

    suspend fun acceptOfferta(login: String, password: String): AcceptOffertaResponse

    suspend fun acceptOffertaByAddress(houseId: Int, flat: Int): AcceptOffertaResponse

    suspend fun getOffices(): OfficesResponse

    suspend fun recoveryOptions(
        contract: String
    ): RecoveryOptionsResponse

    suspend fun confirmCodeRecovery(
        contract: String,
        code: String
    ): ConfirmCodeRecoveryResponse

    suspend fun sentCodeRecovery(
        contract: String,
        contractId: String
    ): SentCodeRecoveryResponse

    suspend fun registerQR(
        url: String
    ): QRResponse

    suspend fun getRoommate(): RoommateResponse

    suspend fun access(
        flatId: Int,
        guestPhone: String?,
        type: String?,
        expire: String?,
        clientId: String?
    ): AccessResponse

    suspend fun resend(
        flatId: Int,
        guestPhone: String
    ): ResendResponse

    suspend fun plogDays(
        flatId: Int,
        events: Set<Int>? = null
    ): PlogDaysResponse

    suspend fun plog(
        flatId: Int,
        day: String
    ): PlogResponse

    suspend fun camMap(): CamMapResponse

    suspend fun getPlaces(): PlacesCctvResponse

    suspend fun getCameras(): CameraCctvResponse

    suspend fun getContracts(): ContractsResponse

    suspend fun setParentControl(clientId: Int): ParentControlResponse

    suspend fun activateLimit(contractId: Int): ActivateLimitResponse
}
