package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.PayRepository
import ru.madbrains.domain.model.response.PayPrepareResponse
import ru.madbrains.domain.model.response.PayProcessResponse
import ru.madbrains.domain.model.response.PaymentDoResponse
import ru.madbrains.domain.model.response.PaymentsListResponse

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

    suspend fun payProcess(paymentId: String, sbId: String): PayProcessResponse {
        return repository.payProcess(paymentId, sbId)
    }

    suspend fun paymentDo(
        merchant: String,
        returnUrl: String,
        paymentToken: String,
        amount: String,
        orderNumber: String
    ): PaymentDoResponse? {
        return repository.paymentDo(
            merchant,
            returnUrl,
            paymentToken,
            amount,
            orderNumber
        )
    }
}
