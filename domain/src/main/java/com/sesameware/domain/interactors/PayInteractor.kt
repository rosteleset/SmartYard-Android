package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.PayRepository
import com.sesameware.domain.model.response.PayPrepareResponse
import com.sesameware.domain.model.response.PaymentsListResponse
import com.sesameware.domain.model.response.PayRegisterResponse

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
class PayInteractor(
    private val repository: PayRepository
) {
    suspend fun getPaymentsList(): PaymentsListResponse {
        return repository.getPaymentsList()
    }

    suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse {
        return repository.payPrepare(clientId, amount)
    }

    suspend fun payRegister(
        orderNumber: String,
        amount: Int
    ): PayRegisterResponse {
        return repository.payRegister(
            orderNumber,
            amount
        )
    }
}
