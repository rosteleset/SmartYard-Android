package ru.madbrains.smartyard.ui.reg.tel

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_number_reg.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R

class NumberRegFragment : Fragment() {

    private var mPhoneNumber: String = ""
    private val mViewModel by viewModel<NumberRegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_number_reg, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNumbersEditText()
        ivExit.setOnClickListener {
            activity?.finish()
        }
        mViewModel.localErrorsSink.observe(
            viewLifecycleOwner,
            EventObserver { error ->
                if (error.status == ErrorStatus.TOO_MANY_REQUESTS) {
                    mViewModel.goToNext(mPhoneNumber, this)
                } else {
                    toggleError(true, error.status.messageId)
                }
            }
        )
    }

    private fun setupNumbersEditText() {
        tel1?.focus()

        tel1?.addTextChangedListener {
            checkToSmsReg()
            if (it?.length == 3) {
                tel2?.requestFocus()
            }
        }

        tel2?.addTextChangedListener {
            checkToSmsReg()
            if (it?.length == 3) {
                tel3?.requestFocus()
            }
        }

        tel2.setOnKeyListener { _, keyCode, event ->
            var r = false
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && tel2.text?.isEmpty() == true) {
                val text: String = tel1.text.toString()
                if (text.isNotEmpty()) {
                    tel1.setText(text.substring(0, text.length - 1))
                    tel1.setSelection(text.length - 1)
                }
                tel1.requestFocus()
                r = true
            }
            r
        }

        tel3?.addTextChangedListener {
            checkToSmsReg()
        }

        tel3.setOnKeyListener { _, keyCode, event ->
            var r = false
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && tel3.text?.isEmpty() == true) {
                val text: String = tel2.text.toString()
                if (text.isNotEmpty()) {
                    tel2.setText(text.substring(0, text.length - 1))
                    tel2.setSelection(text.length - 1)
                }
                tel2.requestFocus()
                r = true
            }
            r
        }
    }

    private fun checkToSmsReg() {
        toggleError(false)
        mPhoneNumber = tel1.text.toString() + tel2.text.toString() + tel3.text.toString()
        if (mPhoneNumber.length == 10) {
            mViewModel.requestSmsCode(mPhoneNumber, this)
        }
    }

    private fun toggleError(error: Boolean, @StringRes resId: Int? = null) {
        tvError.isVisible = error
        if (error) {
            resId?.let { id ->
                tvError.setText(id)
            }
        }
    }
}
