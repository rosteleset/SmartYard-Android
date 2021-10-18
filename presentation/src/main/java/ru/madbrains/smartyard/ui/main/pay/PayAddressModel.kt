package ru.madbrains.smartyard.ui.main.pay

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
@Parcelize
data class PayAddressModel(
    val address: String = "",
    var accounts: List<Account> = listOf()
) : Parcelable {
    @Parcelize
    data class Account(
        val balance: Float = 0.0f,
        val blocked: String = "",
        val bonus: Float = 0.0f,
        val clientId: String = "",
        val clientName: String = "",
        val contractName: String = "",
        val contractPayName: String = "",
        val services: List<String> = listOf(),
        val payAdvice: Float = 0.0f,
        val lcab: String = "",
        val lcabPay: String = ""
    ) : Parcelable {

        fun getBalanceToRub(): String {
            return "${balance.toString().replace(".",",")} ₽"
        }

        fun getPayAdviceToRub(): String {
            return "${payAdvice.toString().replace(".",",")} ₽"
        }
    }
}
