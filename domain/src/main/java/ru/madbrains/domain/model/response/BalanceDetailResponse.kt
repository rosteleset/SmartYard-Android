package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias BalanceDetailResponse = ApiResult<List<BalanceDetailItem>>?

enum class DetailBalanceType {
    PLUS, MINUS
}


data class BalanceDetailItem(
    @Json(name = "type") val _type: String,
    @Json(name = "title") val title: String,
    @Json(name = "date") val date: String,
    @Json(name = "summa") val summa: Float,
) {
    val type: DetailBalanceType
        get() {
            return when (_type) {
                "payments" -> {
                    DetailBalanceType.PLUS
                }

                else -> {
                    DetailBalanceType.MINUS
                }
            }
        }
}