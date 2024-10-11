package ru.madbrains.smartyard.ui.main.pay.yocassa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentYocassaPayBinding
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.DataForPay
import ru.madbrains.smartyard.ui.main.address.PayButtonType
import ru.yoomoney.sdk.kassa.payments.Checkout
import ru.yoomoney.sdk.kassa.payments.Checkout.RESULT_ERROR
import ru.yoomoney.sdk.kassa.payments.Checkout.createConfirmationIntent
import ru.yoomoney.sdk.kassa.payments.Checkout.createSavedCardTokenizeIntent
import ru.yoomoney.sdk.kassa.payments.Checkout.createTokenizationResult
import ru.yoomoney.sdk.kassa.payments.Checkout.createTokenizeIntent
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.Amount
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavePaymentMethod
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavedBankCardPaymentParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.TestParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.UiParameters
import timber.log.Timber
import java.math.BigDecimal
import java.util.Currency
import java.util.Properties


class YocassaPayFragment : Fragment() {
    lateinit var binding: FragmentYocassaPayBinding
    private val mViewModel by sharedViewModel<AddressViewModel>()

    private var apiKey = ""
    private var shopId = ""

    private var summa: BigDecimal = BigDecimal(0)
    private var title = ""
    private var customerId = ""
    private var typePay = ""
    private var bindingId = ""
    private var merchant = ""
    private var confirmationUrl = ""
    private var notify = ""
    private var email = ""
    private var phone = ""
    private var isAutoPay = false
    private var isSaveCard = false


    private fun destroyFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentYocassaPayBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            phone = getString("phone") ?: ""
            notify = getString("notify") ?: ""
            email = getString("email") ?: ""
            summa = getDouble("summa").toBigDecimal()
            title = getString("title") ?: ""
            merchant = getString("merchant") ?: ""
            customerId = getString("customerId") ?: ""
            typePay = getString("method") ?: ""
            bindingId = getString("bindingId") ?: ""
            confirmationUrl = getString("confirmationUrl") ?: ""
            isAutoPay = getBoolean("isAutoPay")
            isSaveCard = getBoolean("isSaveCard")
        }
        when (merchant) {
            "layka" -> {
                apiKey = getString(R.string.API_YOCASSA_LAYKA_KEY)
                shopId = getString(R.string.SHOP_ID_YOCASSA_LAYKA_KEY)
            }

            "centra" -> {
                apiKey = getString(R.string.API_YOCASSA_CENTRA_KEY)
                shopId = getString(R.string.SHOP_ID_YOCASSA_CENTRA_KEY)
            }
        }
        startPay()
    }


    private fun startPay() {
        when (typePay) {
            PayButtonType.VISA.paymentSystem,
            PayButtonType.MASTER_CARD.paymentSystem,
            PayButtonType.MIR.paymentSystem -> {
                savedCardPay()
            }

            PayButtonType.SECURE.paymentSystem -> {
                start3DSecure()
            }

            PayButtonType.NEW_CARD.paymentSystem -> {
                startTokenize()
            }
        }
    }

    private fun savedCardPay() {
        mViewModel.payAuto(
            merchant = merchant,
            contractTitle = title,
            summa = summa.toDouble(),
            bindingId = bindingId,
            notifyMethod = notify,
            email = email
        )
        destroyFragment()
    }

    private fun startSavedCardTokenize() {
        val parameters = SavedBankCardPaymentParameters(
            amount = Amount(summa, Currency.getInstance("RUB")),
            title = title,
            subtitle = "",
            clientApplicationKey = apiKey,
            shopId = shopId,
            paymentMethodId = bindingId,
            savePaymentMethod = SavePaymentMethod.OFF
        )
        val customUi =
            UiParameters(
                showLogo = false
            )
        val intent = createSavedCardTokenizeIntent(
            context = requireContext(),
            savedBankCardPaymentParameters = parameters,
            uiParameters = customUi
        )
        startForResultSavedCard.launch(intent)
    }

    private val startForResultSavedCard =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val data = result.data?.let { createTokenizationResult(it) }
                    if (data != null) {
                        mViewModel.payAuto(
                            merchant = merchant,
                            contractTitle = title,
                            summa = summa.toDouble(),
                            bindingId = bindingId,
                            notifyMethod = notify,
                            email = email,
                        )
                    }
                    destroyFragment()
                }
                else -> {
                    destroyFragment()
                }
            }
        }


    private fun startTokenize() {
        val paymentParameters = PaymentParameters(
            amount = Amount(summa, Currency.getInstance("RUB")),
            title = "Оплата по договору $title",
            subtitle = "",
            clientApplicationKey = apiKey,
            shopId = shopId,
            savePaymentMethod = if (isSaveCard) SavePaymentMethod.ON else SavePaymentMethod.OFF,
            paymentMethodTypes = setOf(PaymentMethodType.BANK_CARD),
            customerId = if (isSaveCard) phone else null
        )
        val customUi = UiParameters(showLogo = false)
        val intent = createTokenizeIntent(
            context = requireContext(),
            paymentParameters = paymentParameters,
            uiParameters = customUi
        )
        startForResult.launch(intent)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.let { createTokenizationResult(it) }
                if (data != null) {
                    mViewModel.mobilePay(
                        merchant = merchant,
                        token = data.paymentToken,
                        contractTitle = title,
                        summa = summa.toDouble(),
                        description = "Платеж по договору: $title",
                        saveAuto = isAutoPay,
                        saveCard = isSaveCard
                    )
                }
                destroyFragment()
            } else {
                destroyFragment()
            }
        }

    private fun start3DSecure() {
        val intent = createConfirmationIntent(
            requireContext(),
            confirmationUrl,
            PaymentMethodType.BANK_CARD,
            testParameters = TestParameters(showLogs = true)
        )
        startForResult3DSecure.launch(intent)
    }

    private val startForResult3DSecure =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val orderId = mViewModel.payState.value?.orderId
                    Thread.sleep(2000)
                    mViewModel.checkPay(orderId = orderId)
                }

                Activity.RESULT_CANCELED -> {
                    return@registerForActivityResult
                }

                Checkout.RESULT_ERROR -> {
                    val errorCode = result.data?.getIntExtra(Checkout.EXTRA_ERROR_CODE, -1)
                    val errorDescription =
                        result.data?.getStringExtra(Checkout.EXTRA_ERROR_DESCRIPTION)
                    val errorUrl = result.data?.getStringExtra(Checkout.EXTRA_ERROR_FAILING_URL)
                    Timber.tag(TAG_CASSA)
                        .d("startForResult3DSecure RESULT_ERROR code:$errorCode description:$errorDescription url:$errorUrl")
                }
            }
            destroyFragment()
        }


    companion object {
        private const val TAG_CASSA = "YocassaPayFragment"
    }
}
