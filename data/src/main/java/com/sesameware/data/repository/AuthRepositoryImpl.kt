package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.AuthRepository
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.request.AppVersionRequest
import com.sesameware.domain.model.request.ConfirmCodeRequest
import com.sesameware.domain.model.request.GetServicesRequest
import com.sesameware.domain.model.request.OpenDoorRequest
import com.sesameware.domain.model.request.RegisterPushTokenRequest
import com.sesameware.domain.model.request.RequestCodeRequest
import com.sesameware.domain.model.request.SendNameRequest
import com.sesameware.domain.model.request.UserNotificationRequest
import com.sesameware.domain.model.response.AppVersionResponse
import com.sesameware.domain.model.response.ConfirmCodeResponse
import com.sesameware.domain.model.response.GetServicesResponse
import com.sesameware.domain.model.response.OpenDoorResponse
import com.sesameware.domain.model.response.RegisterPushTokenResponse
import com.sesameware.domain.model.response.RequestCodeResponse
import com.sesameware.domain.model.response.SendNameResponse
import com.sesameware.domain.model.response.UserNotificationResponse
import com.sesameware.domain.model.response.ProvidersListResponse
import com.sesameware.domain.model.response.ProviderConfigResponse

class AuthRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : AuthRepository, BaseRepository(moshi) {
    override suspend fun providers(): ProvidersListResponse {
        return safeApiCall {
            teledomApi.providers()
        }
    }

    override suspend fun registerPushToken(
        token: String
    ): RegisterPushTokenResponse {
        return safeApiCall {
            teledomApi.registerPushToken(
                DataModule.BASE_URL + "user/registerPushToken",
                RegisterPushTokenRequest(token)).getResponseBody()
        }
    }

    override suspend fun requestCode(
        userPhone: String
    ): RequestCodeResponse {
        return safeApiCall {
            teledomApi.requestCode(
                DataModule.BASE_URL + "user/requestCode",
                RequestCodeRequest(userPhone)).getResponseBody()
        }
    }

    override suspend fun confirmCode(
        userPhone: String,
        smsCode: String
    ): ConfirmCodeResponse {
        return safeApiCall {
            teledomApi.confirmCode(
                DataModule.BASE_URL + "user/confirmCode",
                ConfirmCodeRequest(userPhone, smsCode))
        }
    }

    override suspend fun checkPhone(
        userPhone: String
    ): ConfirmCodeResponse {
        return safeApiCall {
            teledomApi.checkPhone(
                DataModule.BASE_URL + "user/checkPhone",
                RequestCodeRequest(userPhone))
        }
    }

    override suspend fun sendName(
        name: String,
        patronymic: String?
    ): SendNameResponse {
        return safeApiCall {
//            TODO: Simulate errorBody
//            import okhttp3.MediaType.Companion.toMediaTypeOrNull
//                    import okhttp3.ResponseBody.Companion.toResponseBody
//                    import retrofit2.HttpException
//                    import retrofit2.Response
//            val errorBody = "{\"errors\": [\"Unexpected parameter\"]}".
//                toResponseBody("application/json".toMediaTypeOrNull())
//            throw HttpException(Response.error<Any>(422, errorBody))

            teledomApi.sendName(
                DataModule.BASE_URL + "user/sendName",
                SendNameRequest(name, patronymic)).getResponseBody()
        }
    }

    override suspend fun openDoor(
        domophoneId: Int,
        doorId: Int?
    ): OpenDoorResponse {
        return safeApiCall {
            teledomApi.openDoor(
                DataModule.BASE_URL + "address/openDoor",
                OpenDoorRequest(domophoneId, doorId)).getResponseBody()
        }
    }

    override suspend fun getServices(id: Int): GetServicesResponse {
        return safeApiCall {
            teledomApi.getServices(
                DataModule.BASE_URL + "geo/getServices",
                GetServicesRequest(id)).getResponseBody()
        }
    }

    override suspend fun appVersion(version: String): AppVersionResponse {
        return safeApiCall {
            teledomApi.appVersion(
                DataModule.BASE_URL + "user/appVersion",
                AppVersionRequest(version, "android")).getResponseBody()
        }
    }

    override suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse {
        return safeApiCall {
            teledomApi.userNotification(
                DataModule.BASE_URL + "user/notification",
                UserNotificationRequest(money?.value, enable?.value))
        }
    }

    override suspend fun getOptions(): ProviderConfigResponse {
        return safeApiCall {
            teledomApi.getOptions(DataModule.BASE_URL + "ext/options")
        }
    }
}
