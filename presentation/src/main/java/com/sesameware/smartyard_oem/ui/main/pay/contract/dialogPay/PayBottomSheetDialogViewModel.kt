package com.sesameware.smartyard_oem.ui.main.pay.contract.dialogPay

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.PayInteractor
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 26.05.2020.
 */
class PayBottomSheetDialogViewModel(
    private val payInteractor: PayInteractor
) : GenericViewModel() {
    private val _closeBottomDialog = MutableLiveData<Event<Unit>>()
    val closeBottomDialog: LiveData<Event<Unit>>
        get() = _closeBottomDialog

    fun sberPay(amount: String, clientId: String, context: Context) {
        Timber.d("__sber amount = $amount;  clientId = $clientId")
        viewModelScope.withProgress {
            val a = amount.toFloat() * 100
            val rubInKopecks = a.toInt().toString()
            val payPrepare = payInteractor.payPrepare(clientId, rubInKopecks)
            val sberRegisterDo = payInteractor.payRegister(
                payPrepare.data,
                a.toInt()
            )
            if (sberRegisterDo?.data?.formUrl?.isNotEmpty() == true) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(sberRegisterDo.data.formUrl)
                context.startActivity(intent)
                _closeBottomDialog.postValue(Event(Unit))
            }
        }
    }
}
