package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias ProviderConfigResponse = ApiResult<ProviderConfig>?

data class ProviderConfig(
    //основное меню приложения (по умолчанию адреса, уведомления, дополнительно)
    @Json(name = "mainMenu") val mainMenu: MutableList<String>? = mutableListOf(
        MAIN_MENU_ADDRESSES,
        //MAIN_MENU_NOTIFICATIONS,
        MAIN_MENU_ADDITIONAL),

    //видеонаблюдение (по умолчанию нет)
    @Json(name = HAS_CCTV) val _hasCCTV: String? = "f",

    //городские камеры (по умолчанию нет)
    @Json(name = HAS_CITY_CAMS) val _hasCityCams: String? = "f",

    //события (по умолчанию нет)
    @Json(name = HAS_EVENTS) val _hasEvents: String? = "f",

    //система распознавания лиц (по умолчанию нет)
    @Json(name = HAS_FRS) val _hasFRS: String? = "f",

    //работа с заявками (по умолчанию нет)
    @Json(name = HAS_ISSUES) val _hasIssues: String? = "f",

    //платежи
    @Json(name = HAS_PAYMENTS) val _hasPayments: String? = "f",

    //URL для платежей
    @Json(name = PAYMENTS_URL) val paymentsUrl: String? = null,

    //номер телефона техподдержки
    @Json(name = SUPPORT_PHONE) val supportPhone: String? = null,
) {
    val hasCCTV: Boolean
        get() = _hasCCTV == "t"

    val hasCityCams: Boolean
        get() = _hasCityCams == "t"

    val hasEvents: Boolean
        get() = _hasEvents == "t"

    val hasFRS: Boolean
        get() = _hasFRS == "t"

    val hasIssues: Boolean
        get() = _hasIssues == "t"

    val hasPayments: Boolean
        get() = _hasPayments == "t"

    companion object {
        //типы авторизации
        const val AUTH_TYPE_SMS = "sms"
        const val AUTH_TYPE_OUTGOING_CALL = "outgoingCall"
        const val AUTH_TYPE_INCOMING_CALL = "incomingCall"

        //основное меню приложения
        const val MAIN_MENU_ADDRESSES = "addresses"
        const val MAIN_MENU_NOTIFICATIONS = "notifications"
        const val MAIN_MENU_CHAT = "chat"
        const val MAIN_MENU_PAYMENTS = "payments"
        const val MAIN_MENU_ADDITIONAL = "additional"

        //видеонаблюдение
        const val HAS_CCTV = "cctv"

        //городские камеры
        const val HAS_CITY_CAMS = "cityCams"

        //события
        const val HAS_EVENTS = "events"

        //события
        const val HAS_FRS = "frs"

        //работа с заявками
        const val HAS_ISSUES = "issues"

        //платежи
        const val HAS_PAYMENTS = "payments"

        //URL для платежей
        const val PAYMENTS_URL = "paymentsUtl"

        //номер телефона техподдержки
        const val SUPPORT_PHONE = "supportPhone"
    }
}
