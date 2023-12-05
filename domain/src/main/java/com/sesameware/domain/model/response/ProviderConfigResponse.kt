package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ProviderConfigResponse = ApiResult<ProviderConfig>?

data class ProviderConfig(
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
    @Json(name = CHAT_URL) val chatUrl: String? = null,
    @Json(name = CHAT_OPTIONS) val chatOptions: ChatOptions? = null,

    //настройки TimeZone
    @Json(name = TIME_ZONE) val timeZone: String? = null,

    //настройки гостевого доступа
    @Json(name = GUEST_ACCESS) val _guestAccess: String = GUEST_ACCESS_TURN_ON_ONLY,

    //представление камер
    @Json(name = CCTV_VIEW) val _cctvView: String = CCTV_VIEW_LIST,

    //активная вкладка
    @Json(name = ACTIVE_TAB) val activeTab: String = TAB_ADDRESSES,
) {
    val hasChat: Boolean
        get() = _hasChat == "t" || chatUrl?.isNotEmpty() == true

    val hasCityCams: Boolean
        get() = _hasCityCams == "t"

    val hasIssues: Boolean
        get() = _hasIssues == "t"

    val hasPayments: Boolean
        get() = _hasPayments == "t" || paymentsUrl?.isNotEmpty() == true

    data class ChatOptions(
        @Json(name = CHAT_ID) val id: String? = null,
        @Json(name = CHAT_DOMAIN) val domain: String? = null,
        @Json(name = CHAT_TOKEN) val token: String? = null,
    )

    val guestAccess: GuestAccessType get() = GuestAccessType.getType(_guestAccess)

    val cctvView: CCTVViewTypeType get() = CCTVViewTypeType.getType(_cctvView)

    companion object {
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
        const val CHAT_URL = "chatUrl"
        const val CHAT_OPTIONS = "chatOptions"
        const val CHAT_ID = "id"
        const val CHAT_DOMAIN = "domain"
        const val CHAT_TOKEN = "token"

        //настройки TimeZone
        const val TIME_ZONE = "timeZone"

        //настройки гостевого доступа
        const val GUEST_ACCESS = "guestAccess"
        const val GUEST_ACCESS_TURN_ON_ONLY = "turnOnOnly"
        const val GUEST_ACCESS_TURN_ON_AND_OFF = "turnOnAndOff"

        //представление камер
        const val CCTV_VIEW = "cctvView"
        const val CCTV_VIEW_LIST = "list"
        const val CCTV_VIEW_TREE = "tree"

        //вкладки
        const val ACTIVE_TAB = "activeTab"
        const val TAB_ADDRESSES = "addresses"
        const val TAB_NOTIFICATIONS = "notifications"
        const val TAB_CHAT = "chat"
        const val TAB_PAY = "pay"
        const val TAB_MENU = "menu"
    }
}

enum class GuestAccessType {
    TURN_ON_ONLY,
    TURN_ON_AND_OFF;

    companion object {
        fun getType(type: String): GuestAccessType {
            return when (type) {
                ProviderConfig.GUEST_ACCESS_TURN_ON_AND_OFF -> TURN_ON_AND_OFF
                else -> TURN_ON_ONLY
            }
        }
    }
}

enum class CCTVViewTypeType {
    LIST,
    TREE;

    companion object {
        fun getType(type: String): CCTVViewTypeType {
            return when (type) {
                ProviderConfig.CCTV_VIEW_TREE -> TREE
                else -> LIST
            }
        }
    }
}
