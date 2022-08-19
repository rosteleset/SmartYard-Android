package com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.afterTextChanged
import com.sesameware.smartyard_oem.databinding.FragmentRestoreAccessBinding

class RestoreAccessFragment : Fragment() {
    private var _binding: FragmentRestoreAccessBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<RestoreAccessViewModel>()
    private lateinit var adapter: RestoreAdapter
    private var contactId: String = ""
    private var contactName: String = ""
    private var contractNumber = ""

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
        initRecycler()
        arguments?.let {
            contractNumber = RestoreAccessFragmentArgs.fromBundle(it).contractNumber
        }
        binding.etContractNumber.setText(contractNumber)
        binding.btnRecovery.isEnabled = binding.etContractNumber.text?.isNotEmpty() == true
        binding.etContractNumber.addTextChangedListener {
            binding.btnRecovery.isEnabled = it?.isNotEmpty() == true
        }
        binding.btnRecovery.setOnClickListener {
            mViewModel.recoveryOptions(binding.etContractNumber.text.toString())
        }

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.btnCodeConfirm.setOnClickListener {
            mViewModel.sentCodeRecovery(binding.etContractNumber.text.toString(), contactId)
        }

        mViewModel.navigationToCodeSms.observe(
            viewLifecycleOwner,
            EventObserver {
                val action =
                    RestoreAccessFragmentDirections.actionRestoreAccessFragmentToCodeSmsRestoreFragment(
                        binding.etContractNumber.text.toString(), contactId, contactName
                    )
                this.findNavController().navigate(action)
            }
        )

        binding.etContractNumber.afterTextChanged {
            adapter.items = emptyList()
            adapter.notifyDataSetChanged()
            binding.btnRecovery.isVisible = true
            binding.btnCodeConfirm.isVisible = false
        }

        mViewModel.recoveryList.observe(
            viewLifecycleOwner,
            Observer {
                adapter.items = it.map { RecoveryModel(it.contact, it.id, false) }
                adapter.notifyDataSetChanged()
                binding.btnRecovery.isVisible = false
                binding.btnCodeConfirm.isVisible = true
                binding.btnCodeConfirm.isEnabled = false
            }
        )
    }

    private fun initRecycler() {

        activity?.let {
            binding.recoveryOptionsRv.layoutManager =
                LinearLayoutManager(context, RecyclerView.VERTICAL, false)

            adapter = RestoreAdapter(
                it
            ) { _, id, name ->
                adapter.notifyDataSetChanged()
                binding.btnRecovery.isVisible = false
                binding.btnCodeConfirm.isVisible = true
                binding.btnCodeConfirm.isEnabled = true
                contactId = id
                contactName = name
            }
        }
        binding.recoveryOptionsRv.adapter = adapter

        binding.recoveryOptionsRv.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    override fun onHiddenChanged(hidden: Boolean) {
        activity?.window?.setSoftInputMode(
            if (hidden)
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            else
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
        super.onHiddenChanged(hidden)
    }

    override fun onStop() {
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreAccessBinding.inflate(inflater, container, false)
        return binding.root
    }
}
