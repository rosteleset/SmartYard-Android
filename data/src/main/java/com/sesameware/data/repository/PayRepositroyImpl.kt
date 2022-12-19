package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.PayRepository
import com.sesameware.domain.model.request.PayPrepareRequest
import com.sesameware.domain.model.request.PayProcessRequest
import com.sesameware.domain.model.request.PaymentDoRequest
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
class PayRepositroyImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : PayRepository, BaseRepository(moshi) {
    override suspend fun getPaymentsList(): PaymentsListResponse {
        return safeApiCall {
            teledomApi.getPaymentsList(DataModule.BASE_URL + "user/getPaymentsList").getResponseBody()
        }
    }

    override suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse {
        return safeApiCall {
            teledomApi.payPrepare(
                DataModule.BASE_URL + "pay/prepare",
                PayPrepareRequest(clientId, amount))
        }
    }

    override suspend fun payProcess(paymentId: String, sbId: String): PayProcessResponse {
        return safeApiCall {
            teledomApi.payProcess(
                DataModule.BASE_URL + "pay/process",
                PayProcessRequest(paymentId, sbId))
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
