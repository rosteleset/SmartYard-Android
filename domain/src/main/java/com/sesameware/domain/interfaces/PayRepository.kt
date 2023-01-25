package com.sesameware.domain.interfaces

import com.sesameware.domain.model.response.PayPrepareResponse
import com.sesameware.domain.model.response.PayRegisterResponse
import com.sesameware.domain.model.response.PaymentsListResponse

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
interface PayRepository {
    suspend fun getPaymentsList(): PaymentsListResponse

    suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse

    suspend fun payRegister(
        orderNumber: String,
        amount: Int
    ): PayRegisterResponse
}
