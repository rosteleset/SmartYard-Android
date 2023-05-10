package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
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

class AuthRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : AuthRepository, BaseRepository(moshi) {
    override suspend fun registerPushToken(
        token: String
    ): RegisterPushTokenResponse {
        return safeApiCall {
            lantaApi.registerPushToken(RegisterPushTokenRequest(token)).getResponseBody()
        }
    }

    override suspend fun requestCode(
        userPhone: String
    ): RequestCodeResponse {

        println(lantaApi.requestCode(RequestCodeRequest(userPhone)).getResponseBody())

        return safeApiCall {
            lantaApi.requestCode(RequestCodeRequest(userPhone)).getResponseBody()
        }

    }

    override suspend fun confirmCode(
        userPhone: String,
        smsCode: String
    ): ConfirmCodeResponse {
        return safeApiCall {
            lantaApi.confirmCode(ConfirmCodeRequest(userPhone, smsCode))
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

            lantaApi.sendName(SendNameRequest(name, patronymic)).getResponseBody()
        }
    }

    override suspend fun openDoor(
        domophoneId: Int,
        doorId: Int?
    ): OpenDoorResponse {
        return safeApiCall {
            lantaApi.openDoor(OpenDoorRequest(domophoneId, doorId)).getResponseBody()
        }
    }

    override suspend fun getServices(id: Int): GetServicesResponse {
        return safeApiCall {
            lantaApi.getServices(GetServicesRequest(id)).getResponseBody()
        }
    }

    override suspend fun appVersion(version: String): AppVersionResponse {
        return safeApiCall {
            lantaApi.appVersion(AppVersionRequest(version, "android")).getResponseBody()
        }
    }
    override suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse {
        return safeApiCall {
            lantaApi.userNotification(UserNotificationRequest(money?.value, enable?.value))
        }
    }
}
