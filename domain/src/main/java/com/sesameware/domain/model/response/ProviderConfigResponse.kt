package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ProviderConfigResponse = ApiResult<ProviderConfig>?

data class ProviderConfig(
    //уведомления
    @Json(name = HAS_NOTIFICATIONS) val _hasNotifications: String? = "f",

    //чат
    @Json(name = HAS_CHAT) val _hasChat: String? = "f",

    //городские камеры (по умолчанию нет)
    @Json(name = HAS_CITY_CAMS) val _hasCityCams: String? = "f",

    //работа с заявками (по умолчанию нет)
    @Json(name = HAS_ISSUES) val _hasIssues: String? = "f",

    //платежи
    @Json(name = HAS_PAYMENTS) val _hasPayments: String? = "f",

    //URL для платежей
    @Json(name = PAYMENTS_URL) val paymentsUrl: String? = null,

    //номер телефона техподдержки
    @Json(name = SUPPORT_PHONE) val supportPhone: String? = null,

    //настройки чата
    @Json(name = CHAT_OPTIONS) val chatOptions: ChatOptions? = null,
) {
    val hasNotification: Boolean
        get() = _hasNotifications == "t"

    val hasChat: Boolean
        get() = _hasChat == "t"

    val hasCityCams: Boolean
        get() = _hasCityCams == "t"

    val hasIssues: Boolean
        get() = _hasIssues == "t"

    val hasPayments: Boolean
        get() = _hasPayments == "t"

    data class ChatOptions(
        @Json(name = CHAT_ID) val id: String? = null,
        @Json(name = CHAT_DOMAIN) val domain: String? = null,
        @Json(name = CHAT_TOKEN) val token: String? = null,
    )

    companion object {
        //уведомления
        const val HAS_NOTIFICATIONS = "notifications"

        //чат
        const val HAS_CHAT = "chat"

        //городские камеры
        const val HAS_CITY_CAMS = "cityCams"

        //работа с заявками
        const val HAS_ISSUES = "issues"

        //платежи
        const val HAS_PAYMENTS = "payments"

        //URL для платежей
        const val PAYMENTS_URL = "paymentsUrl"

        //номер телефона техподдержки
        const val SUPPORT_PHONE = "supportPhone"

        //настройки чата
        const val CHAT_OPTIONS = "chatOptions"
        const val CHAT_ID = "id"
        const val CHAT_DOMAIN = "domain"
        const val CHAT_TOKEN = "token"
    }
}
