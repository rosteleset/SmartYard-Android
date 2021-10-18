package ru.madbrains.smartyard.ui.reg.sms

import android.content.res.ColorStateList
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
import kotlinx.android.synthetic.main.fragment_sms_reg.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.eventHandler
import ru.madbrains.smartyard.getColorCompat

/**
 * @author Nail Shakurov
 * Created on 2020-02-04.
 */
class SmsRegFragment : Fragment() {

    private var phoneNumber: String = ""

    private val mViewModel by viewModel<SmsRegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sms_reg, container, false)

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

        tvTel.text = String.format(
            getString(R.string.reg_sms_code_tel), phoneNumber
        )

        tvCorrectNumberTel.setOnClickListener {
            this.findNavController().navigate(R.id.action_smsRegFragment_to_numberRegFragment)
        }

        pin?.focus()

        pin?.addTextChangedListener {
            togglePinLineColor(false)
            toggleError(false)
        }
        pin?.setOnClickListener {
            eventHandler(pin, requireContext())
        }
        pin?.setOnPinEnteredListener {
            mViewModel.confirmCode(phoneNumber, it.toString(), this)
        }

        btnResendСode.setOnClickListener {
            toggleError(false)
            mViewModel.resendCode(phoneNumber)
        }

        mViewModel.time.observe(
            viewLifecycleOwner,
            Observer { time ->
                tvTimer?.text = getString(R.string.reg_sms_send_code_repeat, time)
            }
        )

        mViewModel.resendTimerUp.observe(
            viewLifecycleOwner,
            Observer {
                tvTimer?.isVisible = false
                btnResendСode.isVisible = true
            }
        )

        mViewModel.resendTimerStarted.observe(
            viewLifecycleOwner,
            Observer {
                tvTimer?.isVisible = true
                btnResendСode.isVisible = false
            }
        )
    }

    private fun togglePinLineColor(error: Boolean) {
        if (error) {
            pin.setPinLineColors(ColorStateList.valueOf(resources.getColorCompat(R.color.red_100)))
        } else {
            pin.setPinLineColors(ColorStateList.valueOf(resources.getColorCompat(R.color.black)))
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

    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_NAME = "name"
        const val KEY_PATRONYMIC = "patronymic"
    }
}
