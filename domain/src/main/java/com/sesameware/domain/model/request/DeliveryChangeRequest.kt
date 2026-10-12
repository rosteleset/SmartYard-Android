package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class DeliveryChangeRequest(
    @param:Json(name = "action")
    val action: String = "Jelly.Способ доставки", // Jelly.Способ доставки
    @param:Json(name = "customFields")
    val customFields: List<CustomField> = listOf(),
    @param:Json(name = "key")
    val key: String = "" // REM-418379
) {
    data class CustomField(
        @param:Json(name = "number")
        val number: String = "10941", // 10941
        @param:Json(name = "value")
        val value: String = "" // Курьер
    )
}
