package ru.madbrains.smartyard.ui.main.pay

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
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.android.synthetic.main.fragment_pay.*
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.utils.stateSharedViewModel

class PayAddressFragment : Fragment() {

    private val payViewModel: PayAddressViewModel by stateSharedViewModel()

    lateinit var adapter: ListDelegationAdapter<List<PayAddressModel>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_pay, container, false)
        payViewModel.addressList.observe(
            viewLifecycleOwner,
            EventObserver {
                swipeContainer.isRefreshing = false
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
            viewLifecycleOwner,
            Observer {
                if (!swipeContainer.isRefreshing) progressBarAddress.isVisible = it
                swipeContainer.isRefreshing = false
            }
        )
        payViewModel.navigateToСontractFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                val action = PayAddressFragmentDirections.actionPayFragment2ToPayContractFragment()
                this.findNavController().navigate(action)
            }
        )
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecycler()
        swipeContainer.setOnRefreshListener {
            payViewModel.getPaymentsList()
        }
    }

    override fun onResume() {
        super.onResume()
        payViewModel.getPaymentsList()
    }

    private fun initRecycler() {
        rvAddressPay.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter(
            PayAddressDelegate { position, _ ->
                payViewModel.navigateToPayContractFragment(position)
            }
        )
        rvAddressPay.adapter = adapter
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
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                IntentFilter(
                    BROADCAST_PAY_UPDATE
                )
            )
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }

    companion object {
        const val BROADCAST_PAY_UPDATE = "BROADCAST_PAY_UPDATE"
    }
}
