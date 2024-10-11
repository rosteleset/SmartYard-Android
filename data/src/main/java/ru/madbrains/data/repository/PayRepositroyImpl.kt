package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.PayRepository
import ru.madbrains.domain.model.request.AutoPayRequest
import ru.madbrains.domain.model.request.BalanceDetailRequest
import ru.madbrains.domain.model.request.CardsRequest
import ru.madbrains.domain.model.request.CheckFakeRequest
import ru.madbrains.domain.model.request.MobilePayRequest
import ru.madbrains.domain.model.request.NewFakeRequest
import ru.madbrains.domain.model.request.NewPayRequest
import ru.madbrains.domain.model.request.PayAutoRequest
import ru.madbrains.domain.model.request.PayPrepareRequest
import ru.madbrains.domain.model.request.PayProcessRequest
import ru.madbrains.domain.model.request.PaymentDoRequest
import ru.madbrains.domain.model.request.RemoveAutoPayRequest
import ru.madbrains.domain.model.request.RemoveCardRequest
import ru.madbrains.domain.model.request.SendBalanceDetailRequest
import ru.madbrains.domain.model.request.СheckPayRequest
import ru.madbrains.domain.model.response.AutoPayResponse
import ru.madbrains.domain.model.response.BalanceDetailResponse
import ru.madbrains.domain.model.response.CardsResponse
import ru.madbrains.domain.model.response.CheckFakeResponse
import ru.madbrains.domain.model.response.CheckPayItem
import ru.madbrains.domain.model.response.MobilePayResponse
import ru.madbrains.domain.model.response.NewFakeResponse
import ru.madbrains.domain.model.response.NewPayResponse
import ru.madbrains.domain.model.response.PayAutoResponse
import ru.madbrains.domain.model.response.PayPrepareResponse
import ru.madbrains.domain.model.response.PayProcessResponse
import ru.madbrains.domain.model.response.PaymentDoResponse
import ru.madbrains.domain.model.response.PaymentsListResponse
import ru.madbrains.domain.model.response.RemoveAutoPayResponse
import ru.madbrains.domain.model.response.RemoveCardResponse
import ru.madbrains.domain.model.response.SberRegisterDoReponse
import ru.madbrains.domain.model.response.SberOrderStatusDoResponse
import ru.madbrains.domain.model.response.SendBalancesDetailResponse
import ru.madbrains.domain.model.response.СheckPayResponse

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
class PayRepositroyImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : PayRepository, BaseRepository(moshi) {


    override suspend fun getPaymentsList(): PaymentsListResponse {
        return safeApiCall {
            lantaApi.getPaymentsList().getResponseBody()
        }
    }

    override suspend fun payPrepare(clientId: String, amount: String): PayPrepareResponse {
        return safeApiCall {
            lantaApi.payPrepare(PayPrepareRequest(clientId, amount))
        }
    }

    override suspend fun payProcess(paymentId: String, sbId: String): PayProcessResponse {
        return safeApiCall {
            lantaApi.payProcess(PayProcessRequest(paymentId, sbId))
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
            lantaApi.paymentDo(
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
            lantaApi.sberRegisterDo(
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
            lantaApi.sberOrderStatusDo(
                userName,
                password,
                orderNumber
            ).getResponseBody()
        }
    }

    override suspend fun getBalanceDetail(
        id: String,
        to: String,
        from: String
    ): BalanceDetailResponse {
        return safeApiCall {
            lantaApi.getBalanceDetail(BalanceDetailRequest(id, to, from)).getResponseBody()
        }
    }

    override suspend fun sendBalanceDetail(
        id: Int,
        from: String,
        to: String,
        mail: String
    ): SendBalancesDetailResponse {
        return safeApiCall {
            lantaApi.sendBalanceDetail(SendBalanceDetailRequest(id, from, to, mail))
                .getResponseBody()
        }
    }


    override suspend fun getCards(contractName: String): CardsResponse {
        return safeApiCall {
            lantaApi.getCards(CardsRequest(contractName))
                .getResponseBody()
        }
    }

    override suspend fun mobilePay(
        merchant: String,
        token: String,
        contractTitle: String,
        summa: Double,
        description: String?,
        notifyMethod: String,
        email: String?,
        saveCard: Boolean?,
        saveAuto: Boolean?
    ): MobilePayResponse {
       return safeApiCall {
           lantaApi.mobilePay(MobilePayRequest(merchant, contractTitle, summa, token, description, notifyMethod, email, saveAuto, saveCard)).getResponseBody()
       }
    }

    override suspend fun addAutoPay(merchant: String, bindingId: String?, contractTitle: String?): AutoPayResponse {
        return safeApiCall {
            lantaApi.addAutoPay(AutoPayRequest(merchant, bindingId, contractTitle)).getResponseBody()
        }
    }

    override suspend fun removeAutoPay(merchant: String, bindingId: String?, contractTitle: String?): RemoveAutoPayResponse {
        return safeApiCall {
            lantaApi.removeAutoPay(RemoveAutoPayRequest(merchant, bindingId, contractTitle)).getResponseBody()
        }
    }

    override suspend fun removeCard(merchant: String, bindingId: String): RemoveCardResponse {
        return safeApiCall {
            lantaApi.removeCard(RemoveCardRequest(merchant, bindingId)).getResponseBody()
        }
    }

    override suspend fun checkPay(mdOrder: String?, orderId: String?): СheckPayResponse {
        return safeApiCall {
            lantaApi.checkPay(СheckPayRequest(mdOrder, orderId)).getResponseBody()
        }
    }

    override suspend fun newPay(
        merchant: String,
        contractTitle: String,
        summa: Double,
        returnUrl: String,
        saveCard: String,
        saveAuto: String,
        notifyMethod: String,
        email: String?
    ): NewPayResponse {
        return safeApiCall {
            lantaApi.newPay(NewPayRequest(merchant, contractTitle, summa, returnUrl, saveCard, saveAuto, notifyMethod, email)).getResponseBody()
        }
    }


    override suspend fun payAuto(
        merchant: String,
        contractTitle: String,
        summa: Double,
        bindingId: String,
        notifyMethod: String,
        email: String?,
        description: String?
    ): PayAutoResponse {
        return safeApiCall {
            lantaApi.payAuto(PayAutoRequest(merchant, contractTitle, summa, bindingId, description, notifyMethod, email)).getResponseBody()
        }
    }

    override suspend fun checkFake(
        merchant: String,
        id: String,
        status: Int,
        orderId: String,
        processed: String,
        test: String
    ): CheckFakeResponse {
        return safeApiCall {
            lantaApi.checkFake(CheckFakeRequest(id, merchant, status, orderId, processed, test)).getResponseBody()
        }
    }

    override suspend fun newFake(
        contractTitle: String,
        merchant: String,
        summa: Double,
        description: String?,
        comment: String?,
        notifyMethod: String?,
        email: String?
    ): NewFakeResponse {
        return safeApiCall {
            lantaApi.newFake(NewFakeRequest(contractTitle, merchant, summa, description, comment, notifyMethod, email)).getResponseBody()
        }
    }
}

