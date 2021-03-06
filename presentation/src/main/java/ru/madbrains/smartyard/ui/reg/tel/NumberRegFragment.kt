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
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.databinding.FragmentNumberRegBinding

class NumberRegFragment : Fragment() {
    private var _binding: FragmentNumberRegBinding? = null
    private val binding get() = _binding!!

    private var mPhoneNumber: String = ""
    private val mViewModel by viewModel<NumberRegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNumberRegBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNumbersEditText()
        binding.ivExit.setOnClickListener {
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
        binding.tel1.focus()

        binding.tel1.addTextChangedListener {
            checkToSmsReg()
            if (it?.length == 3) {
                binding.tel2.requestFocus()
            }
        }

        binding.tel2.addTextChangedListener {
            checkToSmsReg()
            if (it?.length == 3) {
                binding.tel3.requestFocus()
            }
        }

        binding.tel2.setOnKeyListener { _, keyCode, event ->
            var r = false
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && binding.tel2.text?.isEmpty() == true) {
                val text: String = binding.tel1.text.toString()
                if (text.isNotEmpty()) {
                    binding.tel1.setText(text.substring(0, text.length - 1))
                    binding.tel1.setSelection(text.length - 1)
                }
                binding.tel1.requestFocus()
                r = true
            }
            r
        }

        binding.tel3?.addTextChangedListener {
            checkToSmsReg()
        }

        binding.tel3.setOnKeyListener { _, keyCode, event ->
            var r = false
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && binding.tel3.text?.isEmpty() == true) {
                val text: String = binding.tel2.text.toString()
                if (text.isNotEmpty()) {
                    binding.tel2.setText(text.substring(0, text.length - 1))
                    binding.tel2.setSelection(text.length - 1)
                }
                binding.tel2.requestFocus()
                r = true
            }
            r
        }
    }

    private fun checkToSmsReg() {
        toggleError(false)
        mPhoneNumber = binding.tel1.text.toString() + binding.tel2.text.toString() + binding.tel3.text.toString()
        if (mPhoneNumber.length == 10) {
            mViewModel.requestSmsCode(mPhoneNumber, this)
        }
    }

    private fun toggleError(error: Boolean, @StringRes resId: Int? = null) {
        binding.tvError.isVisible = error
        if (error) {
            resId?.let { id ->
                binding.tvError.setText(id)
            }
        }
    }
}
