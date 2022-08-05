package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.PayRepository
import ru.madbrains.domain.model.request.PayPrepareRequest
import ru.madbrains.domain.model.request.PayProcessRequest
import ru.madbrains.domain.model.request.PaymentDoRequest
import ru.madbrains.domain.model.response.PayPrepareResponse
import ru.madbrains.domain.model.response.PayProcessResponse
import ru.madbrains.domain.model.response.PaymentDoResponse
import ru.madbrains.domain.model.response.PaymentsListResponse
import ru.madbrains.domain.model.response.SberRegisterDoReponse
import ru.madbrains.domain.model.response.SberOrderStatusDoResponse

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
class PayRepositroyImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : PayRepository, BaseRepository(moshi) {
    override suspend fun getPaymentsList(): PaymentsListResponse {
        return safeApiCall {
            teledomApi.getPaymentsList().getResponseBody()
        }
    }

    override suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse {
        return safeApiCall {
            teledomApi.payPrepare(PayPrepareRequest(clientId, amount))
        }
    }

    override suspend fun payProcess(paymentId: String, sbId: String): PayProcessResponse {
        return safeApiCall {
            teledomApi.payProcess(PayProcessRequest(paymentId, sbId))
        }
    }

    override suspend fun paymentDo(
        merchant: String,
        returnUrl: String,
        paymentToken: String,
        amount: String,
        orderNumber: String
    ): PaymentDoResponse {
        return safeApiCall {
            teledomApi.paymentDo(
                PaymentDoRequest(
                    merchant = merchant,
                    returnUrl = returnUrl,
                    paymentToken = paymentToken,
                    amount = amount,
                    orderNumber = orderNumber
                )
            ).getResponseBody()
        }
    }

    override suspend fun sberRegisterDo(
        userName: String,
        password: String,
        language: String,
        returnUrl: String,
        failUrl: String,
        orderNumber: String,
        amount: Int
    ): SberRegisterDoReponse {
        return safeApiCall {
            teledomApi.sberRegisterDo(
                userName,
                password,
                language,
                returnUrl,
                failUrl,
                orderNumber,
                amount
            ).getResponseBody()
        }
    }

    override suspend fun sberOrderStatusDo(
        userName: String,
        password: String,
        orderNumber: String
    ): SberOrderStatusDoResponse {
        return safeApiCall {
            teledomApi.sberOrderStatusDo(
                userName,
                password,
                orderNumber
            ).getResponseBody()
        }
    }
}
