package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.AuthRepository
import com.sesameware.domain.model.TF
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.AppVersionResponse
import com.sesameware.domain.model.response.ConfirmCodeResponse
import com.sesameware.domain.model.response.GetServicesResponse
import com.sesameware.domain.model.response.OpenDoorResponse
import com.sesameware.domain.model.response.ProviderConfigResponse
import com.sesameware.domain.model.response.ProvidersListResponse
import com.sesameware.domain.model.response.RegisterPushTokenResponse
import com.sesameware.domain.model.response.RequestCodeResponse
import com.sesameware.domain.model.response.SendNameResponse
import com.sesameware.domain.model.response.UserNotificationResponse

class AuthInteractor(
    private val repository: AuthRepository
) {
    suspend fun providers(): ProvidersListResponse {
        return repository.providers()
    }

    suspend fun registerPushToken(token: String, bundle: String): RegisterPushTokenResponse {
        return repository.registerPushToken(token, bundle)
    }

    suspend fun requestCode(userPhone: String, deviceToken: String): RequestCodeResponse {
        return repository.requestCode(userPhone, deviceToken)
    }

    suspend fun confirmCode(
        userPhone: String,
        smsCode: String,
        deviceToken: String
    ): ConfirmCodeResponse {
        return repository.confirmCode(userPhone, smsCode, deviceToken)
    }

    suspend fun checkPhone(userPhone: String, deviceToken: String) : ConfirmCodeResponse {
        return repository.checkPhone(userPhone, deviceToken)
    }

    suspend fun sendName(name: String, patronymic: String?, last: String?): SendNameResponse {
        return repository.sendName(name, patronymic, last)
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
