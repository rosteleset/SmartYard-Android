package com.sesameware.smartyard_oem.ui.main.pay.contract.dialogPay

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONException
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.GooglePayUtils
import com.sesameware.smartyard_oem.GooglePayUtils.URL_SBER
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ButtomDialogSheetPayBinding
import com.sesameware.smartyard_oem.ui.main.MainActivity.Companion.LOAD_PAYMENT_DATA_REQUEST_CODE
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.main.pay.contract.PayContractFragmentDirections
import com.sesameware.smartyard_oem.ui.openUrl
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
    private var _binding: ButtomDialogSheetPayBinding? = null
    private val binding get() = _binding!!

    private lateinit var mPaymentsClient: PaymentsClient

    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()

    private val payBottomSheetDialogViewModel by viewModel<PayBottomSheetDialogViewModel>()

    private var clientId: String = ""

    private val args: PayBottomSheetDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ButtomDialogSheetPayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            args.contractPayName
            val contractName = args.contractName
            val payAdvice = args.payAdvice
            val lcabPay = args.lcabPay
            clientId = args.clientId
            if (payAdvice == 0.0f) {
                binding.tvRecommendedMoney.isVisible = false
            } else {
                binding.tvRecommendedMoney.text = String.format(
                    getString(R.string.pay_recomended_sum), payAdvice.toString().replace(".", ",")
                )
                binding.etMoneyBalance.setText(payAdvice.toString())
            }
            binding.tvLcabPay.setOnClickListener {
                openUrl(requireActivity(), lcabPay)
            }
            if (lcabPay.isEmpty()) {
                binding.tvLcabPay.isVisible = false
            }
            binding.tvNumber.text = "№ $contractName"
        }

        //задаем максимальное количество знаков после запятой
        binding.etMoneyBalance.inputFilterDecimal(8, 2)

        binding.etMoneyBalance.addTextChangedListener {
            it?.let {
                view.findViewById<RelativeLayout>(R.id.btnPay)?.isEnabled = it.isNotEmpty()
                binding.btnSber.isEnabled = it.isNotEmpty()
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
                                    paymentData?.toJson()?.let { it1 ->
                                        JSONObject(it1).getJSONObject("paymentMethodData")
                                            .getJSONObject("tokenizationData").getString("token")
                                    }
                                if (token != null) {
                                    payBottomSheetDialogViewModel.pay(
                                        token,
                                        binding.etMoneyBalance.text.toString(),
                                        clientId
                                    )
                                }
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
                view.findViewById<RelativeLayout>(R.id.btnPay)?.isClickable = true
            }
        )

        binding.btnSber.setOnClickListener {
            if (binding.etMoneyBalance.text.toString().toFloat() > 0f) {
                payBottomSheetDialogViewModel.sberPay(binding.etMoneyBalance.text.toString(),
                    clientId, requireContext())
                binding.btnSber.isEnabled = false
            } else {
                onError(getString(R.string.payments_error_2))
            }
        }

        mMainViewModel.sberPayIntent.observe(
            viewLifecycleOwner,
            EventObserver{
                binding.btnSber.isEnabled = true
                if (it?.orderNumber?.isNotEmpty() == true) {
                    onSuccess()
                } else {
                    onError(getString(R.string.payments_error_1))
                }
            })
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
            val task = mPaymentsClient.isReadyToPay(request)
            task.addOnSuccessListener {
                //скрываем кнопку Google Pay до лучших времён
                //setUpGooglePayButton(it)
                setUpGooglePayButton(false)
            }
            task.addOnFailureListener {
                Timber.d("debug_google ${it.message}")
                setUpGooglePayButton(false)
            }
        }
    }

    private fun setUpGooglePayButton(isVisible: Boolean) {
        if (isVisible) {
            view?.findViewById<RelativeLayout>(R.id.btnPay)?.visibility = View.VISIBLE
            view?.findViewById<RelativeLayout>(R.id.btnPay)?.setOnClickListener {
                if (binding.etMoneyBalance.text.toString().toFloat() > 0f) {
                    requestPayment()
                } else {
                    onError(getString(R.string.payments_error_2))
                }
            }
        } else {
            view?.findViewById<RelativeLayout>(R.id.btnPay)?.visibility = View.GONE
            dialog?.let { dialog ->
                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    @SuppressLint("NewApi")
    private fun requestPayment() {
        view?.findViewById<RelativeLayout>(R.id.btnPay)?.isClickable = false
        val paymentDataRequestJson = GooglePayUtils.getPaymentDataRequest(binding.etMoneyBalance.text.toString()) ?: return
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        AutoResolveHelper.resolveTask(
            mPaymentsClient.loadPaymentData(request),
            requireActivity(),
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    private fun onError(errorText: String) {
        this.dismiss()
        val action =
            PayContractFragmentDirections.actionGlobalErrorButtomSheetDialogFragment(
                errorText
            )
        this.findNavController().navigate(action)
    }

    private fun onSuccess() {
        this.dismiss()
        val action = PayContractFragmentDirections.actionGlobalSuccessButtomSheetDialogFragment()
        this.findNavController().navigate(action)
    }
}
