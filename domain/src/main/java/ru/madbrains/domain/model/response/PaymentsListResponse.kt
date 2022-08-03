package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */

typealias PaymentsListResponse = ApiResult<List<PayAddress>>?

data class PayAddress(
    @Json(name = "accounts")
    val accounts: List<Account> = listOf(),
    @Json(name = "address")
    val address: String = "" // Тамбов, ул. Дмитрия Карбышева, дом 5, кв 1
) {
    data class Account(
        @Json(name = "balance")
        val balance: Float = 0.0f, // 0
        @Json(name = "blocked")
        val blocked: String = "", // t
        @Json(name = "bonus")
        val bonus: Float = 0.0f, // 0
        @Json(name = "clientId")
        val clientId: String = "", // 89545
        @Json(name = "clientName")
        val clientName: String = "", // Попова Ирина
        @Json(name = "contractName")
        val contractName: String = "", // ФЛ-84235/20
        @Json(name = "contractPayName")
        val contractPayName: String = "", // 84235
        @Json(name = "services")
        val services: List<String> = listOf(),
        @Json(name = "payAdvice")
        val payAdvice: Float = 0.0f,
        @Json(name = "lcab")
        val lcab: String = "",
        @Json(name = "lcabPay")
        val lcabPay: String = ""
    )
}
