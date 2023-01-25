package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.PayRepository
import com.sesameware.domain.model.request.PayPrepareRequest
import com.sesameware.domain.model.request.PayRegisterRequest
import com.sesameware.domain.model.response.PayPrepareResponse
import com.sesameware.domain.model.response.PaymentsListResponse
import com.sesameware.domain.model.response.PayRegisterResponse

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

    override suspend fun payRegister(
        orderNumber: String,
        amount: Int
    ): PayRegisterResponse {
        return safeApiCall {
            teledomApi.payRegister(
                DataModule.BASE_URL + "pay/register",
                PayRegisterRequest(
                    orderNumber,
                    amount)
            ).getResponseBody()
        }
    }
}
