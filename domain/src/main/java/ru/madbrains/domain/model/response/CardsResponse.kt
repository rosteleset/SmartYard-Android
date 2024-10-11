package ru.madbrains.domain.model.response

import com.squareup.moshi.Json


typealias CardsResponse = ApiResult<CardItem>?

data class CardItem(
    @Json(name = "balance")
    val balance: Double,

    @Json(name = "document_limit")
    val documentLimit: String,

    @Json(name = "document_service_terms")
    val documentServiceTerms: String,

    @Json(name = "cards")
    val cards: List<Card?>,

    @Json(name = "checkSendType")
    val checkSendType: String,

    @Json(name = "email")
    val email: String?,

    @Json(name = "merchant")
    val merchant: String,

    @Json(name = "payAdvice")
    val payAdvice: Double
)

data class Card(
    @Json(name = "autopay")
    val autoPay: Boolean,

    @Json(name = "bindingId")
    val bindingId: String,

    @Json(name = "displayLabel")
    val displayLabel: String,

    @Json(name = "expiryDate")
    val expiryDate: String,

    @Json(name = "maskedPan")
    val maskedPan: String,

    @Json(name = "paymentSystem")
    val paymentSystem: String,

    @Json(name = "paymentWay")
    val paymentWay: String
)