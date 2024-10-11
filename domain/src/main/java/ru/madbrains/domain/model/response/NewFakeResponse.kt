package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias NewFakeResponse = ApiResult<NewFakeResponseItem>?


data class NewFakeResponseItem(
    @Json(name = "comment")
    val comment: String,

    @Json(name = "contractId")
    val contractId: Int,

    @Json(name = "contractTitle")
    val contractTitle: String,

    @Json(name = "createdAt")
    val createdAt: String,

    @Json(name = "description")
    val description: String,

    @Json(name = "id")
    val id: Int,

    @Json(name = "summa")
    val summa: Int,

    @Json(name = "type")
    val type: Int
)