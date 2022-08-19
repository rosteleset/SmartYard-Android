package com.sesameware.domain.interfaces

import retrofit2.http.Field
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.response.GetAddressListResponse
import com.sesameware.domain.model.response.GetSettingsListResponse
import com.sesameware.domain.model.response.IntercomResponse
import com.sesameware.domain.model.response.ResetCodeResponse
import com.sesameware.domain.model.response.AddMyPhoneResponse
import com.sesameware.domain.model.response.OfficesResponse
import com.sesameware.domain.model.response.RecoveryOptionsResponse
import com.sesameware.domain.model.response.ConfirmCodeRecoveryResponse
import com.sesameware.domain.model.response.SentCodeRecoveryResponse
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
