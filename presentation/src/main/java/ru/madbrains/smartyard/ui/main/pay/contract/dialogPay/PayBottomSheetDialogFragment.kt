package ru.madbrains.smartyard.ui.main.pay.contract.dialogPay

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.buttom_dialog_sheet_pay.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.GooglePayUtils
import ru.madbrains.smartyard.GooglePayUtils.URL_SBER
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivity.Companion.LOAD_PAYMENT_DATA_REQUEST_CODE
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.pay.contract.PayContractFragmentDirections
import ru.madbrains.smartyard.ui.openUrl
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * @author Nail Shakurov
 * Created on 21.05.2020.
 */

// extension function to input filter edit text decimal digits
fun EditText.inputFilterDecimal(
    // maximum digits including point and without decimal places
    maxDigitsIncludingPoint: Int,
    maxDecimalPlaces: Int // maximum decimal places
){
    try {
        filters = arrayOf<InputFilter>(
            DecimalDigitsInputFilter(maxDigitsIncludingPoint, maxDecimalPlaces)
        )
    }catch (e: PatternSyntaxException){
        isEnabled = false
        hint = e.message
    }
}

// class to decimal digits input filter
class DecimalDigitsInputFilter(
    maxDigitsIncludingPoint: Int, maxDecimalPlaces: Int
) : InputFilter {
    private val pattern: Pattern = Pattern.compile(
        "[0-9]{0," + (maxDigitsIncludingPoint - 1) + "}+((\\.[0-9]{0,"
                + (maxDecimalPlaces - 1) + "})?)||(\\.)?"
    )

    override fun filter(
        p0: CharSequence?, p1: Int, p2: Int, p3: Spanned?, p4: Int, p5: Int
    ): CharSequence? {
        p3?.apply {
            val matcher: Matcher = pattern.matcher(p3)
            return if (!matcher.matches()) "" else null
        }
        return null
    }
}

class PayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var mPaymentsClient: PaymentsClient

    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()

    private val payBottomSheetDialogViewModel by viewModel<PayBottomSheetDialogViewModel>()

    private var clientId: String = ""

    private val args: PayBottomSheetDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.buttom_dialog_sheet_pay, container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val contractPayName = args.contractPayName
            val contractName = args.contractName
            val payAdvice = args.payAdvice
            val lcabPay = args.lcabPay
            clientId = args.clientId
            if (payAdvice == 0.0f) {
                tvRecommendedMoney.isVisible = false
            } else {
                tvRecommendedMoney.text = String.format(
                    getString(R.string.pay_recomended_sum), payAdvice.toString().replace(".", ",")
                )
                etMoneyBalance.setText(payAdvice.toString())
            }
            tvLcabPay.setOnClickListener {
                openUrl(requireActivity(), lcabPay)
            }
            if (lcabPay.isEmpty()) tvLcabPay.isVisible = false
            tvNumber.text = "№ $contractName"
        }

        //задаем максимальное количество знаков после запятой
        etMoneyBalance?.inputFilterDecimal(8, 2)

        etMoneyBalance?.addTextChangedListener {
            it?.let {
                btnPay?.isEnabled = it.isNotEmpty()
            }
        }

        //делаем так, чтобы при появлении виртуальной клавиатуры было видно поле ввода для суммы
        dialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                sheet.parent.parent.requestLayout()
            }
        }

        payBottomSheetDialogViewModel.navigateToSuccess.observe(
            viewLifecycleOwner,
            EventObserver {
                this.dismiss()
                // открываем веб вью
                val urlString = "$URL_SBER$it"
                val action =
                    PayBottomSheetDialogFragmentDirections.actionPayBottomSheetDialogFragmentToPayWebViewFragment(
                        urlString
                    )
                this.findNavController()
                    .navigate(action)
            }
        )

        mMainViewModel.paySendIntent.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it?.resultCode) {
                    Activity.RESULT_OK -> {
                        it.data?.let {
                            val paymentData = PaymentData.getFromIntent(it)
                            try {
                                val token =
                                    JSONObject(paymentData?.toJson()).getJSONObject("paymentMethodData")
                                        .getJSONObject("tokenizationData").getString("token")
                                payBottomSheetDialogViewModel.pay(
                                    token,
                                    etMoneyBalance.text.toString(),
                                    clientId
                                )
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        onError(getString(R.string.payments_error_1))
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(it.data)?.let { status ->
                            onError(getString(R.string.payments_error) + System.lineSeparator() + CommonStatusCodes.getStatusCodeString(status.statusCode))
                        }
                    }
                }
                btnPay?.isClickable = true
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        mPaymentsClient = GooglePayUtils.createPaymentsClient(requireActivity())
        possiblyShowGooglePayButton()
    }

    @SuppressLint("NewApi")
    private fun possiblyShowGooglePayButton() {
        GooglePayUtils.getIsReadyToPayRequest().let { res ->
            val request = IsReadyToPayRequest.fromJson(res.toString())
            request?.let { req ->
                val task = mPaymentsClient.isReadyToPay(req)
                task.addOnCompleteListener { completedTask ->
                    completedTask.getResult(ApiException::class.java)?.let {
                        if (it) {
                            setUpGooglePayButton()
                        } else {
                            btnPay?.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setUpGooglePayButton() {
        btnPay?.visibility = View.VISIBLE
        btnPay?.setOnClickListener {
            if (etMoneyBalance.text.toString().toFloat() > 0f) {
                requestPayment()
            } else {
                onError(getString(R.string.payments_error_2))
            }
        }
    }

    @SuppressLint("NewApi")
    private fun requestPayment() {
        btnPay?.isClickable = false
        val paymentDataRequestJson = GooglePayUtils.getPaymentDataRequest(etMoneyBalance.text.toString()) ?: return
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        if (request != null) {
            AutoResolveHelper.resolveTask(
                mPaymentsClient.loadPaymentData(request),
                requireActivity(),
                LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        }
    }

    private fun onError(errorText: String) {
        this.dismiss()
        val action =
            PayContractFragmentDirections.actionGlobalErrorButtomSheetDialogFragment(
                errorText
            )
        this.findNavController().navigate(action)
    }
}
