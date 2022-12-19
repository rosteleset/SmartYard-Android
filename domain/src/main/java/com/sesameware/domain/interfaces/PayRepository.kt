package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.PayPrepareResponse
import com.sesameware.domain.model.response.PayProcessResponse
import com.sesameware.domain.model.response.PaymentDoResponse
import com.sesameware.domain.model.response.PaymentsListResponse
import com.sesameware.domain.model.response.SberRegisterDoReponse
import com.sesameware.domain.model.response.SberOrderStatusDoResponse

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

    suspend fun sberRegisterDo(
        userName: String,
        password: String,
        language: String,
        returnUrl: String,
        failUrl: String,
        orderNumber: String,
        amount: Int
    ): SberRegisterDoReponse

    suspend fun sberOrderStatusDo(
        userName: String,
        password: String,
        orderNumber: String
    ): SberOrderStatusDoResponse
}
