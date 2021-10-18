package ru.madbrains.smartyard.ui.main.burger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_call_to_support.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R

class CallToSupportFragment : BottomSheetDialogFragment() {

    private val viewModel: BurgerViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_call_to_support, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCloseSupport.setOnClickListener {
            dismiss()
        }

        llOrderCallback.setOnClickListener {
            viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.ORDER_CALLBACK)
            dismiss()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.dialNumber.observe(
            viewLifecycleOwner,
            {
                it?.let { dialNumber ->
                    if (dialNumber.isNotEmpty()) {
                        tvCallToSupport.text = resources.getString(R.string.burger_call_support_by_phone, dialNumber)
                        pbCallToSupport.visibility = View.GONE

                        //когда получили номер, тогда и создаем обработчик
                        llCallToSupport.setOnClickListener {
                            viewModel.chosenSupportOption.postValue(BurgerViewModel.SupportOption.CALL_TO_SUPPORT_BY_PHONE)
                            dismiss()
                        }
                    }
                }
            }
        )
    }
}