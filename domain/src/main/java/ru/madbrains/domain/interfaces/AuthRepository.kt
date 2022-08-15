package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.TF
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

interface AuthRepository {
    suspend fun providers(): ProvidersListResponse
    suspend fun registerPushToken(token: String): RegisterPushTokenResponse
    suspend fun requestCode(userPhone: String): RequestCodeResponse
    suspend fun confirmCode(userPhone: String, smsCode: String): ConfirmCodeResponse
    suspend fun sendName(name: String, patronymic: String?): SendNameResponse
    suspend fun openDoor(domophoneId: Int, doorId: Int?): OpenDoorResponse
    suspend fun getServices(id: Int): GetServicesResponse
    suspend fun appVersion(version: String): AppVersionResponse
    suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse
    suspend fun getOptions(): ProviderConfigResponse
}
