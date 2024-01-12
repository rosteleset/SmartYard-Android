package com.sesameware.domain.model.request

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

/**
 * @author Nail Shakurov
 * Created on 31/03/2020.
 */
data class CreateIssuesRequest(
    @Json(name = "issue")
    val issue: Issue?,
    @Json(name = "customFields")
    val customFields: CustomFields?,
    @Json(name = "actions")
    val actions: List<String>?
) {
    enum class TypeAction(var list: List<String> = listOf()) {
        ACTION1(listOf("Начать работу", "Позвонить")),
        ACTION2(listOf("Начать работу", "Передать в офис")),
        ACTION3(listOf("Начать работу", "Менеджеру ВН"))
    }

    data class CustomFields(
        @Json(name = "10011")
        val x10011: String? = null, // -1
        @Json(name = "11841")
        val x11841: String? = null, // $телефон, введенный пользователем$
        @Json(name = "12440")
        val x12440: String? = null, // Приложение
        @Json(name = "10743")
        var x10743: String? = null, // широта 12312312
        @Json(name = "10744")
        var x10744: String? = null, // долгота 123123123
        @Json(name = "10941")
        val x10941: Int? = null, // 10581
        @Json(name = "11840")
        var x11840: String? = null, // текущая дата и время
    )

    data class Issue(
        @Json(name = "description")
        val description: String = "", // ФИО: Иван Иванов Телефон: 89176194895, Адрес: test, Услуги: test
        @Json(name = "project")
        val project: String = "REM", // REM
        @Json(name = "summary")
        val summary: String = "", // Авто: Звонок с приложения
        @Json(name = "type")
        val type: Long = 0 // 31231231231231231231231232
    )

    data class Builder(
        var issue: Issue? = null,
        var customFields: CustomFields? = null,
        var actions: List<String>? = null
    ) {
        fun issue(issue: Issue) = apply { this.issue = issue }
        fun customFields(customFields: CustomFields) = apply { this.customFields = customFields }
        fun actions(actions: List<String>) = apply { this.actions = actions }

        fun build() = CreateIssuesRequest(issue, customFields, actions)
    }
}

const val ISSUE_REQUEST_CALLBACK = "requestCallback"
const val ISSUE_REQUEST_FRAGMENT = "requestFragment"
const val ISSUE_REMOVE_ADDRESS = "removeAddress"
const val ISSUE_CONNECT_SERVICES_NO_COMMON = "connectServicesNoCommon"
const val ISSUE_CONNECT_SERVICES_HAS_COMMON = "connectServicesHasCommon"
const val ISSUE_CONNECT_SERVICES_NO_NETWORK = "connectServicesNoNetwork"
const val ISSUE_REQUEST_QR_CODE_OFFICE = "requestQRCodeOffice"
const val ISSUE_REQUEST_QR_CODE_COURIER = "requestQRCodeCourier"
const val ISSUE_REQUEST_CREDENTIALS = "requestCredentials"

const val ISSUE_ACTION_CLOSE = "close"
const val ISSUE_ACTION_CHANGE_QR_DELIVERY_TYPE = "changeQRDeliveryType"

const val DELIVERY_OFFICE = "office"
const val DELIVERY_COURIER = "courier"

enum class IssueTypeV2(val type: String) {
    @Json(name = ISSUE_REQUEST_CALLBACK) REQUEST_CALLBACK(ISSUE_REQUEST_CALLBACK),
    @Json(name = ISSUE_REQUEST_FRAGMENT) REQUEST_FRAGMENT(ISSUE_REQUEST_FRAGMENT),
    @Json(name = ISSUE_REMOVE_ADDRESS) REMOVE_ADDRESS(ISSUE_REMOVE_ADDRESS),
    @Json(name = ISSUE_CONNECT_SERVICES_NO_COMMON) CONNECT_SERVICES_NO_COMMON(ISSUE_CONNECT_SERVICES_NO_COMMON),
    @Json(name = ISSUE_CONNECT_SERVICES_HAS_COMMON) CONNECT_SERVICES_HAS_COMMON(ISSUE_CONNECT_SERVICES_HAS_COMMON),
    @Json(name = ISSUE_CONNECT_SERVICES_NO_NETWORK) CONNECT_SERVICES_NO_NETWORK(ISSUE_CONNECT_SERVICES_NO_NETWORK),
    @Json(name = ISSUE_REQUEST_QR_CODE_OFFICE) REQUEST_QR_CODE_OFFICE(ISSUE_REQUEST_QR_CODE_OFFICE),
    @Json(name = ISSUE_REQUEST_QR_CODE_COURIER) REQUEST_QR_CODE_COURIER(ISSUE_REQUEST_QR_CODE_COURIER),
    @Json(name = ISSUE_REQUEST_CREDENTIALS) REQUEST_CREDENTIALS(ISSUE_REQUEST_CREDENTIALS)
}

data class CreateIssuesRequestV2(
    @Json(name = "type") val type: IssueTypeV2,
    @Json(name = "userName") val userName: String? = null,
    @Json(name = "inputAddress") val inputAddress: String? = null,
    @Json(name = "services") val services: String? = null,
    @Json(name = "comments") val comments: String? = null,
    @Json(name = "cameraId") val cameraId: String? = null,
    @Json(name = "cameraName") val cameraName: String? = null,
    @Json(name = "fragmentDate") val fragmentDate: String? = null,
    @Json(name = "fragmentTime") val fragmentTime: String? = null,
    @Json(name = "fragmentDuration") val fragmentDuration: String? = null
)
