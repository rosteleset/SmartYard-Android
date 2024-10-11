package ru.madbrains.domain.model.response

import com.squareup.moshi.Json


typealias NewPayResponse = ApiResult<NewPayItem>?

data class NewPayItem(
    @Json( name = "formUrl")
    val formUrl: String,

    @Json(name = "orderId")
    val orderId: String,

    @Json(name = "token")
    val token: String
)