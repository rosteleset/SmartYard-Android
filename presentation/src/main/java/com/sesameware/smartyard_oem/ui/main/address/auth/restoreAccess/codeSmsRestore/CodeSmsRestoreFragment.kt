package com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess.codeSmsRestore

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.R.string
import com.sesameware.smartyard_oem.databinding.FragmentCodeSmsRestoreBinding
import com.sesameware.smartyard_oem.eventHandler
import com.sesameware.smartyard_oem.getColorCompat
import com.sesameware.smartyard_oem.isEmailCharacter

class CodeSmsRestoreFragment : Fragment() {
    private var _binding: FragmentCodeSmsRestoreBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<CodeSmsRestoreViewModel>()
    private var contract = ""
    private var contactName = ""
    private var contactId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  {
        _binding = FragmentCodeSmsRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.let {
            contract = CodeSmsRestoreFragmentArgs.fromBundle(it).contract
            contactName = CodeSmsRestoreFragmentArgs.fromBundle(it).contactName
            contactId = CodeSmsRestoreFragmentArgs.fromBundle(it).contactId
        }

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.tvTel.text =
            if (contactName.isEmailCharacter()) {
                String.format(
                    getString(string.restore_access_input_code_email), contactName
                )
            } else {
                String.format(
                    getString(string.restore_access_input_code_phone), contactName
                )
            }
        mViewModel.confirmError.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it.httpCode == 403) {
                    toggleError(true, R.string.reg_sms_error_code)
                } else {
                    toggleError(true, it.status.messageId)
                }
                togglePinLineColor(true)
            }
        )

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

        binding.btnResendCode.setOnClickListener {
            mViewModel.sentCodeRecovery(contract, contactId)
        }

        mViewModel.sentCodeError.observe(
            viewLifecycleOwner,
            Observer { error ->
                Toast.makeText(
                    context,
                    "Error: ${error?.peekContent()?.errorData?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        mViewModel.time.observe(
            viewLifecycleOwner,
            Observer { time ->
                binding.tvTimer.text = getString(string.reg_sms_send_code_repeat, time)
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

        binding.pin.setOnPinEnteredListener {
            mViewModel.confirmCodeRecovery(contract, binding.pin.text.toString())
        }

        mViewModel.navigationToDialog.observe(
            viewLifecycleOwner,
            EventObserver {
                showDialog()
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

    private fun showDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(getString(R.string.restore_access_dialog_ok_title))
            .setPositiveButton(getString(R.string.restore_access_dialog_ok_yes)) { _, _ ->
                if (this.isAdded) {
                    NavHostFragment.findNavController(this).navigate(R.id.action_codeSmsRestoreFragment_to_authFragment)
                }
            }
            .show()
    }
}
