package ru.madbrains.smartyard.ui.main.pay.contract.dialogPay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.data.DataModule
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.domain.model.CommonError
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.domain.model.response.ApiResultNull
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.GooglePayUtils
import timber.log.Timber
import java.io.UnsupportedEncodingException

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
class PayBottomSheetDialogViewModel(
    private val payInteractor: PayInteractor
) : GenericViewModel() {

    private val _navigateToSuccess = MutableLiveData<Event<String>>()
    val navigateToSuccess: LiveData<Event<String>>
        get() = _navigateToSuccess

    private val language = "ru"
    private val returnUrl = "https://mycentra.ru/"
    private val sberUrl = "centra://sber"
//    private val sberUrl = "https://mycentra.ru/payments/check"

    fun pay(token: String, amount: String, clientId: String) {
        viewModelScope.withProgress {
            // цену умножить на 100
            val a = amount.toFloat() * 100
            val rubInKopecks = a.toInt().toString()
            val tokenBase24 = encodeString(token)
            val payPrepare = payInteractor.payPrepare(clientId, rubInKopecks)
            val paymentDo = payInteractor.paymentDo(
                merchant = GooglePayUtils.MERCHANT_NAME,
                returnUrl = returnUrl,
                paymentToken = tokenBase24,
                amount = rubInKopecks,
                orderNumber = payPrepare.data
            )
            Timber.d("payPrepare: " + payPrepare + "paymentDo: " + paymentDo)
            if (paymentDo?.success == false && paymentDo.data == null) {
                globalData.globalErrorsSink.value = Event(
                    CommonError(
                        Throwable(paymentDo.error.message, null),
                        status = ErrorStatus.OTHER,
                        errorData = ApiResultNull(
                            paymentDo.error.code,
                            paymentDo.error.message,
                            paymentDo.error.description
                        )
                    )
                )
            } else {
                var process = payInteractor.payProcess(payPrepare.data, paymentDo?.data?.orderId!!)
                _navigateToSuccess.value = Event(paymentDo.data?.orderId!!)
            }
        }
    }

    private fun encodeString(s: String): String {
        var data = ByteArray(0)
        try {
            data = s.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } finally {
            return Base64.encodeToString(data, Base64.DEFAULT)
        }
    }

    fun sberPay(amount: String, clientId: String, context: Context) {
        Timber.d("__sber amount = $amount;  clientId = $clientId")
        viewModelScope.withProgress {
            val a = amount.toFloat() * 100
            val rubInKopecks = a.toInt().toString()
            val payPrepare = payInteractor.payPrepare(clientId, rubInKopecks)
            val returnUrl = sberUrl + "?clientId=" + clientId.toInt() + "&orderNumber=" + payPrepare.data
            val failUrl = sberUrl  + "?clientId=" + clientId.toInt()
            Timber.d("__sber returnUrl = $returnUrl   failUrl = $failUrl")
            val sberRegisterDo = payInteractor.sberRegisterDo(
                DataModule.sberApiUserName,
                DataModule.sberApiPassword,
                language,
                sberUrl + "?clientId=" + clientId.toInt() + "&orderNumber=" + payPrepare.data,
                sberUrl  + "?clientId=" + clientId.toInt(),
                payPrepare.data,
                a.toInt()
            )
            if (sberRegisterDo?.formUrl?.isNotEmpty() == true) {
                synchronized(DataModule.orderNumberToId) {
                    DataModule.orderNumberToId[payPrepare.data] = sberRegisterDo.orderId ?: ""
                }
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(sberRegisterDo.formUrl)
                context.startActivity(intent)
            }
        }
    }
}
