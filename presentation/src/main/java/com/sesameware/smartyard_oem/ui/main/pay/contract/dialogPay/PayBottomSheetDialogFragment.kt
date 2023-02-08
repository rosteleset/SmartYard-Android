package com.sesameware.smartyard_oem.ui.main.pay.contract.dialogPay

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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.BottomDialogSheetPayBinding
import com.sesameware.smartyard_oem.ui.main.pay.contract.PayContractFragmentDirections
import com.sesameware.smartyard_oem.ui.openUrl
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
    private var _binding: BottomDialogSheetPayBinding? = null
    private val binding get() = _binding!!

    private val payBottomSheetDialogViewModel by viewModel<PayBottomSheetDialogViewModel>()
    private var clientId: String = ""
    private val args: PayBottomSheetDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomDialogSheetPayBinding.inflate(inflater, container, false)
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

        binding.btnSber.setOnClickListener {
            if (binding.etMoneyBalance.text.toString().toFloat() > 0f) {
                payBottomSheetDialogViewModel.sberPay(binding.etMoneyBalance.text.toString(),
                    clientId, requireContext())
                binding.btnSber.isEnabled = false
            } else {
                onError(getString(R.string.payments_error_2))
            }
        }

        payBottomSheetDialogViewModel.closeBottomDialog.observe(
            viewLifecycleOwner,
            EventObserver {
                this.dismiss()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    private fun onError(errorText: String) {
        this.dismiss()
        val action =
            PayContractFragmentDirections.actionGlobalErrorBottomSheetDialogFragment(
                errorText
            )
        this.findNavController().navigate(action)
    }

    private fun onSuccess() {
        this.dismiss()
        val action = PayContractFragmentDirections.actionGlobalSuccessBottomSheetDialogFragment()
        this.findNavController().navigate(action)
    }
}
