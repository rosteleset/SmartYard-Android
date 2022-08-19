package com.sesameware.domain.model.response

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * @author Nail Shakurov
 * Created on 27.05.2020.
 */

typealias PaymentDoResponse = PaymentDo?

open class PaymentDo(
    @Json(name = "error")
    val error: Error = Error(),
    @Json(name = "success")
    val success: Boolean = false, // false
    @Json(name = "data") val data: Data?
) : Serializable {
    data class Error(
        @Json(name = "code")
        val code: Int = -1, // 10
        @Json(name = "description")
        val description: String = "", // Некорректное значение параметра [paymentToken]
        @Json(name = "message")
        val message: String = "" // Некорректное значение параметра [paymentToken]
    )

    data class Data(
        @Json(name = "orderId")
        val orderId: String = "", // 12312312123
        @Json(name = "paReq")
        val paReq: String = "" // 12312312123
    )
}
