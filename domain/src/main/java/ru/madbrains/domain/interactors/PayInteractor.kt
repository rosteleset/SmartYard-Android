package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.PayRepository
import ru.madbrains.domain.model.response.AutoPayResponse
import ru.madbrains.domain.model.response.BalanceDetailResponse
import ru.madbrains.domain.model.response.CardsResponse
import ru.madbrains.domain.model.response.CheckFakeResponse
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
class PayInteractor(
    private val repository: PayRepository
) {

    suspend fun getBalanceDetail(id: String, to: String, from: String): BalanceDetailResponse {
        return repository.getBalanceDetail(id, to, from)
    }

    suspend fun getCards(contractName: String): CardsResponse {
        return repository.getCards(contractName)
    }

    suspend fun mobilePay(
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
        return repository.mobilePay(merchant, token, contractTitle, summa, description, notifyMethod, email, saveCard, saveAuto)
    }

    suspend fun addAutoPay(
        merchant: String,
        bindingId: String?,
        contractTitle: String?
    ): AutoPayResponse {
        return repository.addAutoPay(merchant, bindingId, contractTitle)
    }

    suspend fun removeAutoPay(
        merchant: String,
        bindingId: String?,
        contractTitle: String?
    ): RemoveAutoPayResponse {
        return repository.removeAutoPay(merchant, bindingId, contractTitle)
    }

    suspend fun removeCard(merchant: String, bindingId: String): RemoveCardResponse {
        return repository.removeCard(merchant, bindingId)
    }

    suspend fun newFake(
        contractTitle: String,
        merchant: String,
        summa: Double,
        description: String?,
        comment: String?,
        notifyMethod: String?,
        email: String?
    ): NewFakeResponse {
        return repository.newFake(
            contractTitle,
            merchant,
            summa,
            description,
            comment,
            notifyMethod,
            email
        )
    }

    suspend fun checkFake(
        merchant: String,
        id: String,
        status: Int,
        orderId: String,
        processed: String,
        test: String
    ): CheckFakeResponse {
        return repository.checkFake(merchant, id, status, orderId, processed, test)
    }

    suspend fun checkPay(mdOrder: String?, orderId: String?): СheckPayResponse {
        return repository.checkPay(mdOrder, orderId)
    }

    suspend fun newPay(
        merchant: String,
        contractTitle: String,
        summa: Double,
        returnUrl: String,
        saveCard: String,
        saveAuto: String,
        notifyMethod: String,
        email: String?
    ): NewPayResponse {
        return repository.newPay(
            merchant, contractTitle, summa, returnUrl, saveCard, saveAuto, notifyMethod, email
        )
    }

    suspend fun payAuto(
        merchant: String,
        contractTitle: String,
        summa: Double,
        bindingId: String,
        notifyMethod: String,
        email: String?,
        description: String?
    ): PayAutoResponse {
        return repository.payAuto(
            merchant, contractTitle, summa, bindingId, notifyMethod, email, description
        )
    }

    suspend fun sendBalanceDetail(
        id: Int,
        from: String,
        to: String,
        mail: String
    ): SendBalancesDetailResponse {
        return repository.sendBalanceDetail(id, from, to, mail)
    }

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
    ): PaymentDoResponse {
        return repository.paymentDo(
            merchant,
            returnUrl,
            paymentToken,
            amount,
            orderNumber
        )
    }

    suspend fun sberRegisterDo(
        userName: String,
        password: String,
        language: String,
        returnUrl: String,
        failUrl: String,
        orderNumber: String,
        amount: Int
    ): SberRegisterDoReponse {
        return repository.sberRegisterDo(
            userName,
            password,
            language,
            returnUrl,
            failUrl,
            orderNumber,
            amount
        )
    }

    suspend fun sberOrderStatusDo(
        userName: String,
        password: String,
        orderNumber: String
    ): SberOrderStatusDoResponse {
        return repository.sberOrderStatusDo(
            userName,
            password,
            orderNumber
        )
    }
}
