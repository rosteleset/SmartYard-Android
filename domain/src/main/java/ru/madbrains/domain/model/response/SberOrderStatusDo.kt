package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias SberOrderStatusDoResponse = SberOrderStatusDo?

open class SberOrderStatusDo(
    @Json(name = "actionCode") val actionCode: Int? = null
)
