package ru.madbrains.smartyard.ui.main.pay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentPayBinding
import ru.madbrains.smartyard.ui.main.ChatWoot.FragmentChatWootImage

class PayAddressFragment : Fragment() {
    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private val payViewModel: PayAddressViewModel by sharedStateViewModel()
    private val mWebViewPayViewModel by sharedViewModel<WebViewPayViewModel>()

    lateinit var adapter: ListDelegationAdapter<List<PayAddressModel>>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayBinding.inflate(inflater, container, false)
        val root = binding.root
        val option = mWebViewPayViewModel.options.value
        mWebViewPayViewModel.options.observe(
            viewLifecycleOwner,
            Observer{
                if (!option.isNullOrEmpty()){
                val action = PayAddressFragmentDirections.actionPayFragment3ToWebViewPayFragment()
                this.findNavController().navigate(action)
                }
            }
        )

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
            viewLifecycleOwner,
            Observer {
                if (!binding.swipeContainer.isRefreshing) {
                    binding.progressBarAddress.isVisible = it
                }
                binding.swipeContainer.isRefreshing = false
            }
        )
        payViewModel.navigateToContractFragment.observe(
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
        binding.swipeContainer.setOnRefreshListener {
            payViewModel.getPaymentsList()
        }
    }

    override fun onResume() {
        super.onResume()
        payViewModel.getPaymentsList()
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
