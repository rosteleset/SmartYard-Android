package ru.madbrains.smartyard.ui.reg.sms

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentSmsRegBinding
import ru.madbrains.smartyard.eventHandler
import ru.madbrains.smartyard.getColorCompat

/**
 * @author Nail Shakurov
 * Created on 2020-02-04.
 */
class SmsRegFragment : Fragment() {
    private var _binding: FragmentSmsRegBinding? = null
    private val binding get() = _binding!!

    private var phoneNumber: String = ""

    private val mViewModel by viewModel<SmsRegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmsRegBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@SmsRegFragment.findNavController()
                        .navigate(R.id.action_smsRegFragment_to_numberRegFragment)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel.confirmError.observe(
            viewLifecycleOwner,
            EventObserver {
                toggleError(
                    true,
                    R.string.reg_sms_error_code
                )
                togglePinLineColor(true)
            }
        )
        mViewModel.sendPhoneError.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it.status == ErrorStatus.BAD_RESPONSE) {
                    toggleError(true, R.string.send_sms_error_code)
                } else {
                    toggleError(true, it.status.messageId)
                }
            }
        )

        requireNotNull(arguments).run {
            phoneNumber = requireNotNull(getString(KEY_PHONE_NUMBER))
        }

        binding.tvTel.text = String.format(
            getString(R.string.reg_sms_code_tel), phoneNumber
        )

        binding.tvCorrectNumberTel.setOnClickListener {
            this.findNavController().navigate(R.id.action_smsRegFragment_to_numberRegFragment)
        }

        binding.pin.focus()

        binding.pin.addTextChangedListener {
            togglePinLineColor(false)
            toggleError(false)
        }
        binding.pin.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                eventHandler(binding.pin, requireContext())
            }
        }
        binding.pin.setOnPinEnteredListener {
            mViewModel.confirmCode(phoneNumber, it.toString(), this)
        }

        binding.btnResendCode.setOnClickListener {
            toggleError(false)
            mViewModel.resendCode(phoneNumber)
        }

        mViewModel.time.observe(
            viewLifecycleOwner,
            Observer { time ->
                binding.tvTimer.text = getString(R.string.reg_sms_send_code_repeat, time)
            }
        )

        mViewModel.resendTimerUp.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvTimer.isVisible = false
                binding.btnResendCode.isVisible = true
            }
        )

        mViewModel.resendTimerStarted.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvTimer.isVisible = true
                binding.btnResendCode.isVisible = false
            }
        )
    }

    private fun togglePinLineColor(error: Boolean) {
        if (error) {
            binding.pin.setPinLineColors(ColorStateList.valueOf(resources.getColorCompat(R.color.red_100)))
        } else {
            binding.pin.setPinLineColors(ColorStateList.valueOf(resources.getColorCompat(R.color.black)))
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

    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_NAME = "name"
        const val KEY_PATRONYMIC = "patronymic"
    }
}
