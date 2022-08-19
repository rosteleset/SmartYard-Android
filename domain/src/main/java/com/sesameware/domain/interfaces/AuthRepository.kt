package com.sesameware.domain.interfaces

import com.sesameware.domain.model.TF
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

interface AuthRepository {
    suspend fun providers(): ProvidersListResponse
    suspend fun registerPushToken(token: String): RegisterPushTokenResponse
    suspend fun requestCode(userPhone: String): RequestCodeResponse
    suspend fun confirmCode(userPhone: String, smsCode: String): ConfirmCodeResponse
    suspend fun checkPhone(userPhone: String): ConfirmCodeResponse
    suspend fun sendName(name: String, patronymic: String?): SendNameResponse
    suspend fun openDoor(domophoneId: Int, doorId: Int?): OpenDoorResponse
    suspend fun getServices(id: Int): GetServicesResponse
    suspend fun appVersion(version: String): AppVersionResponse
    suspend fun userNotification(money: TF?, enable: TF?): UserNotificationResponse
    suspend fun getOptions(): ProviderConfigResponse
}
