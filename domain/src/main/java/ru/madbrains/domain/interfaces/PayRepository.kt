package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.response.PayPrepareResponse
import ru.madbrains.domain.model.response.PayProcessResponse
import ru.madbrains.domain.model.response.PaymentDoResponse
import ru.madbrains.domain.model.response.PaymentsListResponse

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
interface PayRepository {

    suspend fun getPaymentsList(): PaymentsListResponse

    suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse

    suspend fun payProcess(paymentId: String, sbId: String): PayProcessResponse

    suspend fun paymentDo(
        merchant: String,
        returnUrl: String,
        paymentToken: String,
        amount: String,
        orderNumber: String
    ): PaymentDoResponse
}
