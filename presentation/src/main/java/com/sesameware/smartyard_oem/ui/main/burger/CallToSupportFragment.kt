package com.sesameware.smartyard_oem.ui.main.burger

import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCallToSupportBinding
import com.sesameware.smartyard_oem.getCountryIso
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CallToSupportFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentCallToSupportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BurgerViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentCallToSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivCloseSupport.setOnClickListener {
            dismiss()
        }

        binding.llOrderCallback.visibility = View.VISIBLE
        binding.llOrderCallback.setOnClickListener {
            viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.ORDER_CALLBACK)
            dismiss()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.dialNumber.observe(
            viewLifecycleOwner
        ) {
            it?.let { dialNumber ->
                if (dialNumber.isNotEmpty()) {
                    val phoneNnumber =
                        resources.getString(R.string.burger_call_support_by_phone, dialNumber)
                    val formattedNumber =
                        PhoneNumberUtils.formatNumber(phoneNnumber, requireContext().getCountryIso())
                    binding.tvCallToSupport.text = formattedNumber

                    binding.pbCallToSupport.visibility = View.GONE

                    //когда получили номер, тогда и создаем обработчик
                    binding.llCallToSupport.setOnClickListener {
                        viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.CALL_TO_SUPPORT_BY_PHONE)
                        dismiss()
                    }
                }
            }
        }
    }
}