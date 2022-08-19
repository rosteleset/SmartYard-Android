package com.sesameware.smartyard_oem.ui.main.address.auth

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
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.R.string
import com.sesameware.smartyard_oem.afterTextChanged
import com.sesameware.smartyard_oem.databinding.FragmentAuthBinding
import com.sesameware.smartyard_oem.ui.showStandardAlert

class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModel<AuthViewModel>()
    private var start = 0
    private var end = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
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
        start = binding.etPassword.selectionStart
        end = binding.etPassword.selectionEnd
        if (binding.etPassword.transformationMethod == PasswordTransformationMethod.getInstance()
        ) {
            binding.ivShowHide.setImageResource(R.drawable.ic_visibility)
            binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
        } else {
            binding.ivShowHide.setImageResource(R.drawable.ic_visibility_off)
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        binding.etPassword.setSelection(start, end)
    }

    private fun setupUi() {
        binding.ivShowHide.setOnClickListener {
            showHidePass()
        }
        binding.btnNoContract.setOnClickListener {
            viewModel.seenWarning()
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_authFragment_to_inputAddressFragment)
        }
        binding.btnSignIn.setOnClickListener {
            viewModel.seenWarning()
            viewModel.signIn(binding.etContractNumber.text.toString(), binding.etPassword.text.toString())
        }
        binding.tvNotRememberPassword.setOnClickListener {
            val action =
                AuthFragmentDirections.actionAuthFragmentToRestoreAccessFragment(binding.etContractNumber.text.toString())
            this.findNavController().navigate(action)
        }
        binding.etContractNumber.afterTextChanged {
            binding.btnSignIn.isEnabled = it.isNotEmpty() && binding.etPassword.text.isNotEmpty()
        }

        binding.etPassword.afterTextChanged {
            binding.btnSignIn.isEnabled = it.isNotEmpty() && binding.etContractNumber.text.isNotEmpty()
        }

        binding.tvRememberAnything.setOnClickListener {
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
