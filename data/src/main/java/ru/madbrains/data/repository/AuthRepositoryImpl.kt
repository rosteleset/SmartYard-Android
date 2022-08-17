package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.DataModule
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.AuthRepository
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.request.AppVersionRequest
import ru.madbrains.domain.model.request.ConfirmCodeRequest
import ru.madbrains.domain.model.request.GetServicesRequest
import ru.madbrains.domain.model.request.OpenDoorRequest
import ru.madbrains.domain.model.request.RegisterPushTokenRequest
import ru.madbrains.domain.model.request.RequestCodeRequest
import ru.madbrains.domain.model.request.SendNameRequest
import ru.madbrains.domain.model.request.UserNotificationRequest
import ru.madbrains.domain.model.response.AppVersionResponse
import ru.madbrains.domain.model.response.ConfirmCodeResponse
import ru.madbrains.domain.model.response.GetServicesResponse
import ru.madbrains.domain.model.response.OpenDoorResponse
import ru.madbrains.domain.model.response.RegisterPushTokenResponse
import ru.madbrains.domain.model.response.RequestCodeResponse
import ru.madbrains.domain.model.response.SendNameResponse
import ru.madbrains.domain.model.response.UserNotificationResponse
import ru.madbrains.domain.model.response.ProvidersListResponse
import ru.madbrains.domain.model.response.ProviderConfigResponse

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
