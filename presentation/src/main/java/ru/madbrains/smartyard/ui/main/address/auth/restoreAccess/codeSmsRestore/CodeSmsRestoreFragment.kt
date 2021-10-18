package ru.madbrains.smartyard.ui.main.address.auth.restoreAccess.codeSmsRestore

import android.app.AlertDialog
import android.content.res.ColorStateList
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
import kotlinx.android.synthetic.main.fragment_code_sms_restore.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.string
import ru.madbrains.smartyard.eventHandler
import ru.madbrains.smartyard.getColorCompat
import ru.madbrains.smartyard.isEmailCharacter

class CodeSmsRestoreFragment : Fragment() {

    private val mViewModel by viewModel<CodeSmsRestoreViewModel>()
    private var contract = ""
    private var contactName = ""
    private var contactId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_code_sms_restore, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.let {
            contract = CodeSmsRestoreFragmentArgs.fromBundle(it).contract
            contactName = CodeSmsRestoreFragmentArgs.fromBundle(it).contactName
            contactId = CodeSmsRestoreFragmentArgs.fromBundle(it).contactId
        }

        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        tvTel.text =
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

        pin?.focus()

        pin?.addTextChangedListener {
            togglePinLineColor(false)
            toggleError(false)
        }

        pin?.setOnClickListener {
            eventHandler(pin, requireContext())
        }

        btnResendСode.setOnClickListener {
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
                tvTimer?.text = getString(string.reg_sms_send_code_repeat, time)
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

        pin?.setOnPinEnteredListener {
            mViewModel.confirmCodeRecovery(contract, pin.text.toString())
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
