package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias CheckFakeResponse = ApiResult<CheckFakeResponseItem>?


data class CheckFakeResponseItem(
    @Json(name = "bindingId")
    val bindingId: String?,

    @Json(name = "confirmationUrl")
    val confirmationUrl: String?,

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

    @Json(name = "errorCode")
    val errorCode: Int,

    @Json(name = "features")
    val features: String?,

    @Json(name = "id")
    val id: Int,

    @Json(name = "orderId")
    val orderId: String,

    @Json(name = "pan")
    val pan: String?,

    @Json(name = "processedAt")
    val processedAt: String,

    @Json(name = "status")
    val status: Int,

    @Json(name = "summa")
    val summa: Int,

    @Json(name = "transactionId")
    val transactionId: Int,

    @Json(name = "type")
    val type: Int
)