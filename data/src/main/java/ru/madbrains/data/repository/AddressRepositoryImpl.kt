package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.AddressRepository
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.request.AccessRequest
import ru.madbrains.domain.model.request.AddMyPhoneRequest
import ru.madbrains.domain.model.request.ConfirmCodeRecoveryRequest
import ru.madbrains.domain.model.request.GetIntercomRequest
import ru.madbrains.domain.model.request.PutIntercomRequest
import ru.madbrains.domain.model.request.QRRequest
import ru.madbrains.domain.model.request.RecoveryOptionsRequest
import ru.madbrains.domain.model.request.ResendRequest
import ru.madbrains.domain.model.request.ResetCodeRequest
import ru.madbrains.domain.model.request.SentCodeRecoveryRequest
import ru.madbrains.domain.model.request.Settings
import ru.madbrains.domain.model.request.PlogDaysRequest
import ru.madbrains.domain.model.request.PlogRequest
import ru.madbrains.domain.model.response.AccessResponse
import ru.madbrains.domain.model.response.AddMyPhoneResponse
import ru.madbrains.domain.model.response.ConfirmCodeRecoveryResponse
import ru.madbrains.domain.model.response.GetAddressListResponse
import ru.madbrains.domain.model.response.GetSettingsListResponse
import ru.madbrains.domain.model.response.IntercomResponse
import ru.madbrains.domain.model.response.OfficesResponse
import ru.madbrains.domain.model.response.QRResponse
import ru.madbrains.domain.model.response.RecoveryOptionsResponse
import ru.madbrains.domain.model.response.ResendResponse
import ru.madbrains.domain.model.response.ResetCodeResponse
import ru.madbrains.domain.model.response.RoommateResponse
import ru.madbrains.domain.model.response.SentCodeRecoveryResponse
import ru.madbrains.domain.model.response.PlogDaysResponse
import ru.madbrains.domain.model.response.PlogResponse
import ru.madbrains.domain.model.response.CamMapResponse

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
class AddressRepositoryImpl(
    private val teledomApi: TeledomApi,

    override val moshi: Moshi
) : AddressRepository, BaseRepository(moshi) {
    override suspend fun getAddressList(): GetAddressListResponse {
        return safeApiCall {
            teledomApi.getAddressList().getResponseBody()
        }
    }

    override suspend fun getSettingsList(): GetSettingsListResponse {
        return safeApiCall {
            teledomApi.getSettingsList().getResponseBody()
        }
    }

    override suspend fun putIntercom(
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
        return safeApiCall {
            teledomApi.putIntercom(
                PutIntercomRequest(
                    flatId,
                    Settings(
                        enableDoorCode?.value,
                        cms?.value,
                        voip?.value,
                        autoOpen,
                        whiteRabbit,
                        paperBill?.value,
                        disablePlog?.value,
                        hiddenPlog?.value,
                        frsDisabled?.value
                    )
                )
            )
        }
    }

    override suspend fun getIntercom(flatId: Int): IntercomResponse {
        return safeApiCall {
            teledomApi.getIntercom(GetIntercomRequest(flatId))
        }
    }

    override suspend fun resetCode(flatId: Int): ResetCodeResponse {
        return safeApiCall {
            teledomApi.resetCode(ResetCodeRequest(flatId))
        }
    }

    override suspend fun getOffices(): OfficesResponse {
        return safeApiCall {
            teledomApi.getOffices()
        }
    }

    override suspend fun recoveryOptions(contract: String): RecoveryOptionsResponse {
        return safeApiCall {
            teledomApi.recoveryOptions(RecoveryOptionsRequest(contract))
                .getResponseBody()
        }
    }

    override suspend fun confirmCodeRecovery(
        contract: String,
        code: String
    ): ConfirmCodeRecoveryResponse {
        return safeApiCall {
            teledomApi.confirmCodeRecovery(ConfirmCodeRecoveryRequest(contract, code))
                .getResponseBody()
        }
    }

    override suspend fun sentCodeRecovery(
        contract: String,
        contractId: String
    ): SentCodeRecoveryResponse {
        return safeApiCall {
            teledomApi.sentCodeRecovery(SentCodeRecoveryRequest(contract, contractId))
                .getResponseBody()
        }
    }

    override suspend fun registerQR(
        url: String
    ): QRResponse {
        return safeApiCall {
            teledomApi.registerQR(QRRequest(url))
        }
    }

    override suspend fun getRoommate(): RoommateResponse {
        return safeApiCall {
            teledomApi.getRoommate()
        }
    }

    override suspend fun access(
        flatId: Int,
        guestPhone: String?,
        type: String?,
        expire: String?,
        clientId: String?
    ): AccessResponse {
        return safeApiCall {
            teledomApi.access(AccessRequest(flatId, guestPhone, type, expire, clientId))
                .getResponseBody()
        }
    }

    override suspend fun resend(flatId: Int, guestPhone: String): ResendResponse {
        return safeApiCall {
            teledomApi.resend(ResendRequest(flatId, guestPhone)).getResponseBody()
        }
    }

    override suspend fun plogDays(flatId: Int, events: Set<Int>): PlogDaysResponse {
        return safeApiCall {
            teledomApi.plogDays(PlogDaysRequest(flatId, events.joinToString())).getResponseBody()
        }
    }

    override suspend fun plog(flatId: Int, day: String): PlogResponse {
        return safeApiCall {
            teledomApi.plog(PlogRequest(flatId, day)).getResponseBody()
        }
    }

    override suspend fun addMyPhone(
        login: String,
        password: String,
        comment: String?,
        notification: String?
    ): AddMyPhoneResponse {
        return safeApiCall {
            teledomApi.addMyPhone(AddMyPhoneRequest(login, password, comment, notification))
                .getResponseBody()
        }
    }

    override suspend fun camMap(): CamMapResponse {
        return safeApiCall {
            teledomApi.camMap().getResponseBody()
        }
    }
}
