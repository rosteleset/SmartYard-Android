package com.sesameware.domain.model.request

import com.squareup.moshi.Json

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
