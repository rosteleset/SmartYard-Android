package ru.madbrains.domain.interfaces

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

interface AuthRepository {
    suspend fun registerPushToken(token: String): RegisterPushTokenResponse
    suspend fun requestCode(userPhone: String): RequestCodeResponse
    suspend fun requestCodePush(userPhone: String, type: String, pushToken: String): RequestCodePushResponse
    suspend fun confirmCode(userPhone: String, smsCode: String, type: String?, requestId: String?): ConfirmCodeResponse
    suspend fun sendName(name: String, patronymic: String?): SendNameResponse
    suspend fun openDoor(domophoneId: Long, doorId: Int?): OpenDoorResponse
    suspend fun getServices(id: Int): GetServicesResponse
    suspend fun appVersion(version: String, platform: String, system: String, device: String): AppVersionResponse
    suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse
    suspend fun logout(): LogOutResponse
}
