package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.AuthRepository
import ru.madbrains.domain.model.TF
import ru.madbrains.domain.model.response.AppVersionResponse
import ru.madbrains.domain.model.response.ConfirmCodeResponse
import ru.madbrains.domain.model.response.GetServicesResponse
import ru.madbrains.domain.model.response.LogOutResponse
import ru.madbrains.domain.model.response.OpenDoorResponse
import ru.madbrains.domain.model.response.RegisterPushTokenResponse
import ru.madbrains.domain.model.response.RequestCodePushResponse
import ru.madbrains.domain.model.response.RequestCodeResponse
import ru.madbrains.domain.model.response.SendNameResponse
import ru.madbrains.domain.model.response.UserNotificationResponse

class AuthInteractor(
    private val repository: AuthRepository
) {
    suspend fun registerPushToken(token: String): RegisterPushTokenResponse {
        return repository.registerPushToken(token)
    }

    suspend fun requestCode(userPhone: String): RequestCodeResponse {
        return repository.requestCode(userPhone)
    }

    suspend fun requestCodePush(userPhone: String, type: String, pushToken: String): RequestCodePushResponse {
        return repository.requestCodePush(userPhone, type, pushToken)
    }

    suspend fun confirmCode(
        userPhone: String,
        smsCode: String,
        type: String?,
        requestId: String?
    ): ConfirmCodeResponse {
        return repository.confirmCode(userPhone, smsCode, type, requestId)
    }

    suspend fun sendName(name: String, patronymic: String?): SendNameResponse {
        return repository.sendName(name, patronymic)
    }

    suspend fun openDoor(domophoneId: Long, doorId: Int? = null): OpenDoorResponse {
        return repository.openDoor(domophoneId, doorId)
    }

    suspend fun getServices(id: Int): GetServicesResponse {
        return repository.getServices(id)
    }

    suspend fun appVersion(version: String, platform: String, system: String, device: String): AppVersionResponse {
        return repository.appVersion(version, platform, system, device)
    }

    suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse {
        return repository.userNotification(money, enable)
    }

    suspend fun logout(): LogOutResponse {
        return repository.logout()
    }
}
