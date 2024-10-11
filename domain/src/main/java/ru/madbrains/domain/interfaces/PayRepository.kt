package ru.madbrains.domain.interfaces

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

    suspend fun getBalanceDetail(id: String, to: String, from: String): BalanceDetailResponse

    suspend fun sendBalanceDetail(id: Int, from: String, to: String, mail: String): SendBalancesDetailResponse

    suspend fun getCards(contractName: String): CardsResponse

    suspend fun mobilePay(merchant: String, token: String, contractTitle: String, summa: Double, description: String?, notifyMethod: String, email: String?, saveCard: Boolean?, saveAuto: Boolean?): MobilePayResponse

    suspend fun addAutoPay(merchant:String, bindingId: String?, contractTitle: String?): AutoPayResponse

    suspend fun removeAutoPay(merchant:String, bindingId: String?, contractTitle: String?): RemoveAutoPayResponse

    suspend fun removeCard(merchant:String, bindingId: String): RemoveCardResponse

    suspend fun checkPay(mdOrder:String?, orderId: String?): СheckPayResponse

    suspend fun newPay(merchant:String, contractTitle: String, summa: Double, returnUrl: String, saveCard: String, saveAuto: String, notifyMethod: String,email: String?): NewPayResponse

    suspend fun payAuto(merchant:String, contractTitle: String, summa: Double, bindingId: String, notifyMethod: String, email: String?, description: String?): PayAutoResponse

    suspend fun checkFake(merchant: String, id: String, status: Int, orderId: String, processed: String, test: String): CheckFakeResponse

    suspend fun newFake(contractTitle: String, merchant: String, summa: Double, description: String?, comment: String?, notifyMethod: String?, email: String?): NewFakeResponse
}
