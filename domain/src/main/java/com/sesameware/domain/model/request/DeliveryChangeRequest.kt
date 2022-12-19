package com.sesameware.domain.model.request

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class DeliveryChangeRequest(
    @Json(name = "action")
    val action: String = "Jelly.Способ доставки", // Jelly.Способ доставки
    @Json(name = "customFields")
    val customFields: List<CustomField> = listOf(),
    @Json(name = "key")
    val key: String = "" // REM-418379
) {
    data class CustomField(
        @Json(name = "number")
        val number: String = "10941", // 10941
        @Json(name = "value")
        val value: String = "" // Курьер
    )
}
