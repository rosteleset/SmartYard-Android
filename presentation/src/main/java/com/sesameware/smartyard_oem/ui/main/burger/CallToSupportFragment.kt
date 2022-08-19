package com.sesameware.smartyard_oem.ui.main.burger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCallToSupportBinding

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

        if (DataModule.providerConfig.hasIssues) {
            binding.llOrderCallback.visibility = View.VISIBLE
            binding.llOrderCallback.setOnClickListener {
                viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.ORDER_CALLBACK)
                dismiss()
            }
        } else {
            binding.llOrderCallback.visibility = View.INVISIBLE
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.dialNumber.observe(
            viewLifecycleOwner
        ) {
            it?.let { dialNumber ->
                if (dialNumber.isNotEmpty()) {
                    binding.tvCallToSupport.text =
                        resources.getString(R.string.burger_call_support_by_phone, dialNumber)
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