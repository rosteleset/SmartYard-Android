package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.AuthRepository
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.response.*

class AuthInteractor(
    private val repository: AuthRepository
) {
    suspend fun providers(): ProvidersListResponse {
        return repository.providers()
    }

    suspend fun registerPushToken(token: String): RegisterPushTokenResponse {
        return repository.registerPushToken(token)
    }

    suspend fun requestCode(userPhone: String): RequestCodeResponse {
        return repository.requestCode(userPhone)
    }

    suspend fun confirmCode(
        userPhone: String,
        smsCode: String
    ): ConfirmCodeResponse {
        return repository.confirmCode(userPhone, smsCode)
    }

    suspend fun checkPhone(userPhone: String) : ConfirmCodeResponse {
        return repository.checkPhone(userPhone)
    }

    suspend fun sendName(name: String, patronymic: String?): SendNameResponse {
        return repository.sendName(name, patronymic)
    }

    suspend fun openDoor(domophoneId: Int, doorId: Int? = null): OpenDoorResponse {
        return repository.openDoor(domophoneId, doorId)
    }

    suspend fun getServices(
        id: Int
    ): GetServicesResponse {
        return repository.getServices(id)
    }

    suspend fun appVersion(version: String): AppVersionResponse {
        return repository.appVersion(version)
    }

    suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse {
        return repository.userNotification(money, enable)
    }

    suspend fun getOptions(): ProviderConfigResponse {
        return repository.getOptions()
    }

    suspend fun phonePattern(): ApiResult<String>? {
        return repository.phonePattern()
    }
}
