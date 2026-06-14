package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.AddressRepository
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.request.AccessRequest
import com.sesameware.domain.model.request.AddMyPhoneRequest
import com.sesameware.domain.model.request.ConfirmCodeRecoveryRequest
import com.sesameware.domain.model.request.GetIntercomRequest
import com.sesameware.domain.model.request.GetTrackedEventsRequest
import com.sesameware.domain.model.request.PutIntercomRequest
import com.sesameware.domain.model.request.QRRequest
import com.sesameware.domain.model.request.RecoveryOptionsRequest
import com.sesameware.domain.model.request.ResendRequest
import com.sesameware.domain.model.request.ResetCodeRequest
import com.sesameware.domain.model.request.SentCodeRecoveryRequest
import com.sesameware.domain.model.request.Settings
import com.sesameware.domain.model.request.PlogDaysRequest
import com.sesameware.domain.model.request.PlogRequest
import com.sesameware.domain.model.request.TrackEventRequest
import com.sesameware.domain.model.request.UntrackEventRequest
import com.sesameware.domain.model.response.AccessResponse
import com.sesameware.domain.model.response.AddMyPhoneResponse
import com.sesameware.domain.model.response.ConfirmCodeRecoveryResponse
import com.sesameware.domain.model.response.GetAddressListResponse
import com.sesameware.domain.model.response.GetSettingsListResponse
import com.sesameware.domain.model.response.GetStoriesResponse
import com.sesameware.domain.model.response.IntercomResponse
import com.sesameware.domain.model.response.OfficesResponse
import com.sesameware.domain.model.response.QRResponse
import com.sesameware.domain.model.response.RecoveryOptionsResponse
import com.sesameware.domain.model.response.ResendResponse
import com.sesameware.domain.model.response.ResetCodeResponse
import com.sesameware.domain.model.response.RoommateResponse
import com.sesameware.domain.model.response.SentCodeRecoveryResponse
import com.sesameware.domain.model.response.PlogDaysResponse
import com.sesameware.domain.model.response.PlogResponse
import com.sesameware.domain.model.response.CamMapResponse
import com.sesameware.domain.model.response.GetTrackedEventsResponse
import com.sesameware.domain.model.response.TrackEventResponse
import com.sesameware.domain.model.response.UntrackEventResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
class AddressRepositoryImpl(
    private val teledomApi: TeledomApi,

    override val moshi: Moshi
) : AddressRepository, BaseRepository(moshi) {

    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun getAddressList(): GetAddressListResponse {
        return safeApiCall {
            teledomApi.getAddressList(DataModule.BASE_URL + "address/getAddressList").getResponseBody()
        }
    }

    override suspend fun getSettingsList(): GetSettingsListResponse {
        return safeApiCall {
            teledomApi.getSettingsList(DataModule.BASE_URL + "address/getSettingsList").getResponseBody()
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
                DataModule.BASE_URL + "address/intercom",
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
            teledomApi.getIntercom(
                DataModule.BASE_URL + "address/intercom",
                GetIntercomRequest(flatId))
        }
    }

    override suspend fun resetCode(flatId: Int): ResetCodeResponse {
        return safeApiCall {
            teledomApi.resetCode(
                DataModule.BASE_URL + "address/resetCode",
                ResetCodeRequest(flatId))
        }
    }

    override suspend fun getOffices(): OfficesResponse {
        return safeApiCall {
            teledomApi.getOffices(DataModule.BASE_URL + "address/offices")
        }
    }

    override suspend fun recoveryOptions(contract: String): RecoveryOptionsResponse {
        return safeApiCall {
            teledomApi.recoveryOptions(
                DataModule.BASE_URL + "user/restore",
                RecoveryOptionsRequest(contract))
                .getResponseBody()
        }
    }

    override suspend fun confirmCodeRecovery(
        contract: String,
        code: String
    ): ConfirmCodeRecoveryResponse {
        return safeApiCall {
            teledomApi.confirmCodeRecovery(
                DataModule.BASE_URL + "user/restore",
                ConfirmCodeRecoveryRequest(contract, code))
                .getResponseBody()
        }
    }

    override suspend fun sentCodeRecovery(
        contract: String,
        contractId: String
    ): SentCodeRecoveryResponse {
        return safeApiCall {
            teledomApi.sentCodeRecovery(
                DataModule.BASE_URL + "user/restore",
                SentCodeRecoveryRequest(contract, contractId))
                .getResponseBody()
        }
    }

    override suspend fun registerQR(
        url: String
    ): QRResponse {
        return safeApiCall {
            teledomApi.registerQR(
                DataModule.BASE_URL + "address/registerQR",
                QRRequest(url))
        }
    }

    override suspend fun getRoommate(): RoommateResponse {
        return safeApiCall {
            teledomApi.getRoommate(DataModule.BASE_URL + "address/getSettingsList")
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
            teledomApi.access(
                DataModule.BASE_URL + "address/access",
                AccessRequest(flatId, guestPhone, type, expire, clientId))
                .getResponseBody()
        }
    }

    override suspend fun resend(flatId: Int, guestPhone: String): ResendResponse {
        return safeApiCall {
            teledomApi.resend(
                DataModule.BASE_URL + "address/resend",
                ResendRequest(flatId, guestPhone)).getResponseBody()
        }
    }

    override suspend fun plogDays(flatId: Int, events: Set<Int>): PlogDaysResponse {
        return safeApiCall {
            teledomApi.plogDays(
                DataModule.BASE_URL + "address/plogDays",
                PlogDaysRequest(flatId, events.joinToString())).getResponseBody()
        }
    }

    override suspend fun plog(flatId: Int, day: String): PlogResponse {
        return safeApiCall {
            teledomApi.plog(
                DataModule.BASE_URL + "address/plog",
                PlogRequest(flatId, day)).getResponseBody()
        }
    }

    override suspend fun addMyPhone(
        login: String,
        password: String,
        comment: String?,
        notification: String?
    ): AddMyPhoneResponse {
        return safeApiCall {
            teledomApi.addMyPhone(
                DataModule.BASE_URL + "user/addMyPhone",
                AddMyPhoneRequest(login, password, comment, notification))
                .getResponseBody()
        }
    }

    override suspend fun camMap(): CamMapResponse {
        return safeApiCall {
            teledomApi.camMap(DataModule.BASE_URL + "cctv/camMap").getResponseBody()
        }
    }

    override suspend fun trackEvent(
        flatId: Int,
        eventType: Int,
        eventDetail: String,
        comments: String
    ): TrackEventResponse {
        return safeApiCall {
            teledomApi.trackEvent(
                DataModule.BASE_URL + "address/trackEvent",
                TrackEventRequest(flatId, eventType, eventDetail, comments))
                .getResponseBody()
        }
    }

    override suspend fun untrackEvent(watcherId: Int): UntrackEventResponse {
        return safeApiCall {
            teledomApi.untrackEvent(
                DataModule.BASE_URL + "address/untrackEvent",
                UntrackEventRequest(watcherId))
                .getResponseBody()
        }
    }

    override suspend fun getTrackedEvents(flatId: Int): GetTrackedEventsResponse {
        return safeApiCall {
            teledomApi.getTrackedEvents(
                DataModule.BASE_URL + "address/getTrackedEvents",
                GetTrackedEventsRequest(flatId))
                .getResponseBody()
        }
    }

    override suspend fun getStories(): GetStoriesResponse {
        return safeApiCall {
            teledomApi.getStories(
                DataModule.BASE_URL + "address/getStories"
            ).getResponseBody()
        }
    }
}
