package ru.madbrains.smartyard.ui.main.address.auth.restoreAccess

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
import kotlinx.android.synthetic.main.fragment_restore_access.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.afterTextChanged

class RestoreAccessFragment : Fragment() {

    private val mViewModel by viewModel<RestoreAccessViewModel>()
    private lateinit var adapter: RestoreAdapter
    private var contactId: String = ""
    private var contactName: String = ""
    private var contractNumber = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
        initRecycler()
        arguments?.let {
            contractNumber = RestoreAccessFragmentArgs.fromBundle(it).contractNumber
        }
        etContractNumber.setText(contractNumber)
        btnRecovery.isEnabled = etContractNumber.text?.isNotEmpty() == true
        etContractNumber.addTextChangedListener {
            btnRecovery.isEnabled = it?.isNotEmpty() == true
        }
        btnRecovery.setOnClickListener {
            mViewModel.recoveryOptions(etContractNumber.text.toString())
        }

        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        btnCodeConfirm.setOnClickListener {
            mViewModel.sentCodeRecovery(etContractNumber.text.toString(), contactId)
        }

        mViewModel.navigationToCodeSms.observe(
            viewLifecycleOwner,
            EventObserver {
                val action =
                    RestoreAccessFragmentDirections.actionRestoreAccessFragmentToCodeSmsRestoreFragment(
                        etContractNumber.text.toString(), contactId, contactName
                    )
                this.findNavController().navigate(action)
            }
        )

        etContractNumber.afterTextChanged {
            adapter.items = emptyList()
            adapter.notifyDataSetChanged()
            btnRecovery.isVisible = true
            btnCodeConfirm.isVisible = false
        }

        mViewModel.recoveryList.observe(
            viewLifecycleOwner,
            Observer {
                adapter.items = it.map { RecoveryModel(it.contact, it.id, false) }
                adapter.notifyDataSetChanged()
                btnRecovery.isVisible = false
                btnCodeConfirm.isVisible = true
                btnCodeConfirm.isEnabled = false
            }
        )
    }

    private fun initRecycler() {

        activity?.let {
            recoveryOptionsRv.layoutManager =
                LinearLayoutManager(context, RecyclerView.VERTICAL, false)

            adapter = RestoreAdapter(
                it
            ) { _, id, name ->
                adapter.notifyDataSetChanged()
                btnRecovery.isVisible = false
                btnCodeConfirm.isVisible = true
                btnCodeConfirm.isEnabled = true
                contactId = id
                contactName = name
            }
        }
        recoveryOptionsRv.adapter = adapter

        recoveryOptionsRv.addItemDecoration(
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
    ): View? = inflater.inflate(R.layout.fragment_restore_access, container, false)
}
