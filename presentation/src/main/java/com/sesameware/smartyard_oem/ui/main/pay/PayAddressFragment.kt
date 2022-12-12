package com.sesameware.smartyard_oem.ui.main.pay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.sesameware.data.DataModule
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentPayBinding

class PayAddressFragment : Fragment() {
    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private val payViewModel: PayAddressViewModel by sharedStateViewModel()
    lateinit var adapter: ListDelegationAdapter<List<PayAddressModel>>
    private var isWeb = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isWeb = DataModule.providerConfig.hasPayments && DataModule.providerConfig.paymentsUrl?.isNotEmpty() == true
        if (isWeb) {
            val action = PayAddressFragmentDirections.actionPayFragment2ToCustomWebViewFragment(
                DataModule.providerConfig.paymentsUrl,
                null,
                resources.getString(R.string.pay_web_title)
            )
            action.hasBackButton = false
            val option = NavOptions.Builder()
                .setPopUpTo(R.id.payFragment2, true)
                .build()
            findNavController().navigate(action, option)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayBinding.inflate(inflater, container, false)
        val root = binding.root
        payViewModel.addressList.observe(
            viewLifecycleOwner,
            EventObserver {
                binding.swipeContainer.isRefreshing = false
                if (it.size == 1) {
                    payViewModel.navigateToPayContractFragment(0)
                } else {
                    if (::adapter.isInitialized) {
                        adapter.items = it
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )
        payViewModel.progress.observe(
            viewLifecycleOwner
        ) {
            if (!binding.swipeContainer.isRefreshing) {
                binding.progressBarAddress.isVisible = it
            }
            binding.swipeContainer.isRefreshing = false
        }
        payViewModel.navigateToContractFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                val action = PayAddressFragmentDirections.actionPayFragment2ToPayContractFragment()
                this.findNavController().navigate(action)
            }
        )
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isWeb) {
            initRecycler()
            binding.swipeContainer.setOnRefreshListener {
                payViewModel.getPaymentsList()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isWeb) {
            payViewModel.getPaymentsList()
        }
    }

    private fun initRecycler() {
        binding.rvAddressPay.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter(
            PayAddressDelegate { position, _ ->
                payViewModel.navigateToPayContractFragment(position)
            }
        )
        binding.rvAddressPay.adapter = adapter
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                payViewModel.getPaymentsList()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (!isWeb) {
            context?.let {
                LocalBroadcastManager.getInstance(it).registerReceiver(
                    receiver,
                    IntentFilter(
                        BROADCAST_PAY_UPDATE
                    )
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isWeb) {
            context?.let {
                LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
            }
        }
    }

    companion object {
        const val BROADCAST_PAY_UPDATE = "BROADCAST_PAY_UPDATE"
    }
}
