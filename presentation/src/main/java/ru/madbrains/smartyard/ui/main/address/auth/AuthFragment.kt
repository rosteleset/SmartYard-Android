package ru.madbrains.smartyard.ui.main.address.auth

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.response.ApiResultNull
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.R.string
import ru.madbrains.smartyard.afterTextChanged
import ru.madbrains.smartyard.databinding.FragmentAuthBinding
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressWebViewFragment
import ru.madbrains.smartyard.ui.main.address.auth.offerta.AcceptOffertaFragment
import ru.madbrains.smartyard.ui.main.address.auth.restoreAccess.RestoreAccessFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVMapFragment
import ru.madbrains.smartyard.ui.main.address.inputAdress.InputAddressFragment
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment
import ru.madbrains.smartyard.ui.showStandardAlert
import timber.log.Timber

class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel by sharedViewModel<AuthViewModel>()
    private val mainActivityViewModel by sharedViewModel<MainActivityViewModel>()

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



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupUi()
        setupObserve()
    }

    private fun setupObserve() {
        viewModel.navigationToAddressAction.observe(
            viewLifecycleOwner,
            EventObserver {
                val intentBroadcast = Intent(AddressWebViewFragment.REFRESH_INTENT)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentBroadcast)
                mainActivityViewModel.bottomNavigateToMain()
                for (i in 0 until parentFragmentManager.backStackEntryCount){
                    parentFragmentManager.popBackStack()
                }

//                NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_global_addressFragment2)
            }
        )

        viewModel.navigationToOffertaAction.observe(
            viewLifecycleOwner,
            EventObserver {
                viewModel.seenWarning()
                val transaction = parentFragmentManager.beginTransaction()
                val newFragment = AcceptOffertaFragment()
                val bundle = Bundle()
                bundle.putString("contractNumber", binding.etContractNumber.text.toString())
                bundle.putString("password", binding.etPassword.text.toString())
                newFragment.arguments = bundle
                transaction.replace(R.id.cl_auth_fragment, newFragment)
                transaction.addToBackStack("AcceptOffertaFragment")
                transaction.commit()
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
            val transaction = parentFragmentManager.beginTransaction()
            val newFragment = InputAddressFragment()
            transaction.add(R.id.cl_auth_fragment, newFragment)
            transaction.addToBackStack("InputAddressFragment")
            transaction.commit()
//            NavHostFragment.findNavController(this)
//                .navigate(R.id.action_authFragment_to_inputAddressFragment)
        }

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSignIn.setOnClickListener {
            viewModel.seenWarning()
//            viewModel.signIn(
//                binding.etContractNumber.text.toString(),
//                binding.etPassword.text.toString()
//            )
//            TODO Новая Логика для добавления договора
            viewModel.checkOfferta(
                binding.etContractNumber.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        binding.tvNotRememberPassword.setOnClickListener {
//            val action =
//                AuthFragmentDirections.actionAuthFragmentToRestoreAccessFragment(binding.etContractNumber.text.toString())
//            this.findNavController().navigate(action)
            val contractName = binding.etContractNumber.text.toString()
            val transaction = parentFragmentManager.beginTransaction()
            val newFragment = RestoreAccessFragment()
            val bundle = Bundle()
            bundle.putString("contractNumber", contractName)
            newFragment.arguments = bundle
            transaction.add(R.id.cl_auth_fragment, newFragment)
            transaction.addToBackStack("authFragment")
            transaction.commit()
        }
        binding.etContractNumber.afterTextChanged {
            binding.btnSignIn.isEnabled = it.isNotEmpty() && binding.etPassword.text.isNotEmpty()
        }

        binding.etPassword.afterTextChanged {
            binding.btnSignIn.isEnabled =
                it.isNotEmpty() && binding.etContractNumber.text.isNotEmpty()
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
