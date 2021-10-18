package ru.madbrains.smartyard.ui.main.pay.contract.dialogPay

import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.domain.model.CommonError
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.domain.model.response.ApiResultNull
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.GooglePayUtils
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

    private val returnUrl = "https://lanta-net.ru/"

    fun pay(token: String, amount: String, clietId: String) {
        viewModelScope.withProgress {
            // цену умножить на 100
            val a = amount.toFloat() * 100
            val rubInKopecks = a.toInt().toString()
            val tokenBase24 = encodeString(token)
            val payPrepare = payInteractor.payPrepare(clietId, rubInKopecks)
            val paymentDo = payInteractor.paymentDo(
                merchant = GooglePayUtils.MERCHANT_NAME,
                returnUrl = returnUrl,
                paymentToken = tokenBase24,
                amount = rubInKopecks,
                orderNumber = payPrepare.data
            )
            Log.d("NAIL", "payPrepare: " + payPrepare + "paymentDo: " + paymentDo)
            if (paymentDo?.success == false && paymentDo?.data == null) {
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
                _navigateToSuccess.value = Event(paymentDo?.data?.orderId!!)
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
}
