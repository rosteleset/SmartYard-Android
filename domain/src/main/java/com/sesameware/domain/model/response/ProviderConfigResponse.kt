package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ProviderConfigResponse = ApiResult<ProviderConfig>?

data class ProviderConfig(
    //чат
    @Json(name = HAS_CHAT) val _hasChat: String? = "f",

    //городские камеры (по умолчанию нет)
    @Json(name = HAS_CITY_CAMS) val _hasCityCams: String? = "f",

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

    //версия заявок
    @Json(name = ISSUES_VERSION) val issuesVersion: String = "1",

    //appeal form name, patronymic and last validation
    @Json(name = VALIDATION_NAME_PATTERN) val validationNamePattern: String = "",
    @Json(name = VALIDATION_PATRONYMIC_PATTERN) val validationPatronymicPattern: String = "",
    @Json(name = VALIDATION_LAST_PATTERN) val validationLastPattern: String = "",

    //address verification UI
    @Json(name = ADDRESS_VERIFICATION_TAB_LAYOUT_VISIBLE) val _addressVerificationTabLayoutVisible: String = "t",
    @Json(name = ADDRESS_VERIFICATION_TAB_1_VISIBLE) val _addressVerificationTab1Visible: String = "t",
    @Json(name = ADDRESS_VERIFICATION_TAB_2_VISIBLE) val _addressVerificationTab2Visible: String = "t",

    //events tracking
    @Json(name = HAS_EVENTS_TRACKING) val _hasEventsTracking: String? = "f",

    //stories
    @Json(name = HAS_STORIES) val _hasStories: String? = "f",

    //entrances view
    @Json(name = ENTRANCES_VIEW) val _entrancesView: String? = null,

    //last name
    @Json(name = USER_HAS_LAST_NAME) val _userHasLastName: String? = "f",

    //privacy policy
    @Json(name = PRIVACY_POLICY) val privacyPolicy: String? = null,
) {
    val hasChat: Boolean
        get() = _hasChat == "t" || chatUrl?.isNotEmpty() == true

    val hasCityCams: Boolean
        get() = _hasCityCams == "t"

    val hasPayments: Boolean
        get() = _hasPayments == "t" || paymentsUrl?.isNotEmpty() == true

    data class ChatOptions(
        @Json(name = CHAT_ID) val id: String? = null,
        @Json(name = CHAT_DOMAIN) val domain: String? = null,
        @Json(name = CHAT_TOKEN) val token: String? = null,
    )

    val guestAccess: GuestAccessType get() = GuestAccessType.getType(_guestAccess)

    val cctvView: CCTVViewTypeType get() = CCTVViewTypeType.getType(_cctvView)

    //address verification UI
    val addressVerificationTabLayoutVisible: Boolean
        get() = _addressVerificationTabLayoutVisible == "t"
    val addressVerificationTab1Visible: Boolean
        get() = _addressVerificationTab1Visible == "t"
    val addressVerificationTab2Visible: Boolean
        get() = _addressVerificationTab2Visible == "t"

    //events tracking
    val hasEventsTracking: Boolean
        get() = _hasEventsTracking == "t"

    //stories
    val hasStories: Boolean
        get() = _hasStories == "t"

    val entrancesView: EntrancesView
        get() = EntrancesView.from(_entrancesView)

    val userHasLastName: Boolean
        get() = _userHasLastName == "t"

    companion object {
        //чат
        const val HAS_CHAT = "chat"

        //городские камеры
        const val HAS_CITY_CAMS = "cityCams"

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
        const val CCTV_USER_DEFINED = "userDefined"

        //вкладки
        const val ACTIVE_TAB = "activeTab"
        const val TAB_ADDRESSES = "addresses"
        const val TAB_NOTIFICATIONS = "notifications"
        const val TAB_CHAT = "chat"
        const val TAB_PAY = "pay"
        const val TAB_MENU = "menu"

        //версия заявок
        const val ISSUES_VERSION = "issuesVersion"

        //appeal form validation
        const val VALIDATION_NAME_PATTERN = "validationNamePattern"
        const val VALIDATION_PATRONYMIC_PATTERN = "validationPatronymicPattern"
        const val VALIDATION_LAST_PATTERN = "validationLastPattern"

        //address verification UI
        const val ADDRESS_VERIFICATION_TAB_LAYOUT_VISIBLE = "addressVerificationTabLayoutVisible"
        const val ADDRESS_VERIFICATION_TAB_1_VISIBLE = "addressVerificationTab1Visible"
        const val ADDRESS_VERIFICATION_TAB_2_VISIBLE = "addressVerificationTab2Visible"

        //events tracking
        const val HAS_EVENTS_TRACKING = "eventsTracking"

        //stories
        const val HAS_STORIES = "stories"

        //entrances view
        const val ENTRANCES_VIEW = "entrancesView"

        //last name
        const val USER_HAS_LAST_NAME = "userHasLastName"

        //privacy policy
        const val PRIVACY_POLICY = "privacyPolicy"
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
    TREE,
    USER_DEFINED;

    companion object {
        fun getType(type: String): CCTVViewTypeType {
            return when (type) {
                ProviderConfig.CCTV_VIEW_TREE -> TREE
                ProviderConfig.CCTV_USER_DEFINED -> USER_DEFINED
                else -> LIST
            }
        }
    }
}

enum class EntrancesView(val value: String) {
    LIST("list"),
    PREVIEW("preview");

    companion object {
        fun from(value: String?): EntrancesView =
            entries.find { it.value == value } ?: LIST
    }
}
