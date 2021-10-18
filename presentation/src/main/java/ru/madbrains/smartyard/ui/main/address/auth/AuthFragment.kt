package ru.madbrains.smartyard.ui.main.address.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_auth.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.string
import ru.madbrains.smartyard.afterTextChanged
import ru.madbrains.smartyard.ui.showStandardAlert

class AuthFragment : Fragment() {

    private val viewModel by viewModel<AuthViewModel>()
    private var start = 0
    private var end = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_auth, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupUi()
        setupObserve()
    }

    private fun setupObserve() {
        viewModel.navigationToAddressAction.observe(
            viewLifecycleOwner,
            EventObserver {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_global_addressFragment2)
            }
        )

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(
                    requireContext(),
                    resources.getString(string.issue_dialog_caption_0),
                    null,
                    false
                )
            }
        )
    }

    private fun showHidePass() {
        start = etPassword.selectionStart
        end = etPassword.selectionEnd
        if (etPassword.transformationMethod == PasswordTransformationMethod.getInstance()
        ) {
            ivShowHide.setImageResource(R.drawable.ic_visibility)
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
        } else {
            ivShowHide.setImageResource(R.drawable.ic_visibility_off)
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        etPassword.setSelection(start, end)
    }

    private fun setupUi() {
        ivShowHide.setOnClickListener {
            showHidePass()
        }
        btnNoContract.setOnClickListener {
            viewModel.seenWarning()
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_authFragment_to_inputAddressFragment)
        }
        btnSignIn.setOnClickListener {
            viewModel.seenWarning()
            viewModel.signIn(etContractNumber.text.toString(), etPassword.text.toString())
        }
        tvNotRememberPassword.setOnClickListener {
            val action =
                AuthFragmentDirections.actionAuthFragmentToRestoreAccessFragment(etContractNumber.text.toString())
            this.findNavController().navigate(action)
        }
        etContractNumber.afterTextChanged {
            btnSignIn.isEnabled = it.isNotEmpty() && etPassword.text.isNotEmpty()
        }

        etPassword.afterTextChanged {
            btnSignIn.isEnabled = it.isNotEmpty() && etContractNumber.text.isNotEmpty()
        }

        tvRememberAnything.setOnClickListener {
            showDialogIssue()
        }
    }

    private fun showDialogIssue() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(resources.getString(R.string.auth_dialog_title))
            .setPositiveButton(resources.getString(R.string.auth_dialog_yes)) { _, _ ->
                viewModel.createIssue()
            }
            .setNegativeButton(resources.getString(R.string.auth_dialog_no)) { _, _ ->
                returnTransition
            }.show()
    }
}
