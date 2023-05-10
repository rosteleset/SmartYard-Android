package ru.madbrains.domain.interfaces

import retrofit2.http.Field
import ru.madbrains.domain.model.TF
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
import ru.madbrains.domain.model.response.ResendResponse
import ru.madbrains.domain.model.response.PlogDaysResponse
import ru.madbrains.domain.model.response.PlogResponse
import ru.madbrains.domain.model.response.CamMapResponse

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
interface AddressRepository {
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

    suspend fun addMyPhone(
        login: String,
        password: String,
        comment: String?,
        notification: String?
    ): AddMyPhoneResponse

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
        events: Set<Int>
    ): PlogDaysResponse

    suspend fun plog(
        flatId: Int,
        day: String
    ): PlogResponse

    suspend fun camMap(): CamMapResponse
}
