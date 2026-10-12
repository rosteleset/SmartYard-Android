package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */

typealias PaymentsListResponse = ApiResult<List<PayAddress>>?

data class PayAddress(
    @param:Json(name = "accounts")
    val accounts: List<Account> = listOf(),
    @param:Json(name = "address")
    val address: String = "" // Тамбов, ул. Дмитрия Карбышева, дом 5, кв 1
) {
    data class Account(
        @param:Json(name = "balance")
        val balance: Float = 0.0f, // 0
        @param:Json(name = "blocked")
        val blocked: String = "", // t
        @param:Json(name = "bonus")
        val bonus: Float = 0.0f, // 0
        @param:Json(name = "clientId")
        val clientId: String = "", // 89545
        @param:Json(name = "clientName")
        val clientName: String = "", // Попова Ирина
        @param:Json(name = "contractName")
        val contractName: String = "", // ФЛ-84235/20
        @param:Json(name = "contractPayName")
        val contractPayName: String = "", // 84235
        @param:Json(name = "services")
        val services: List<String> = listOf(),
        @param:Json(name = "payAdvice")
        val payAdvice: Float = 0.0f,
        @param:Json(name = "lcab")
        val lcab: String = "",
        @param:Json(name = "lcabPay")
        val lcabPay: String = ""
    )
}
