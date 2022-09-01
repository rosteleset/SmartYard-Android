package com.sesameware.smartyard_oem.ui.main.pay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.sesameware.data.DataModule
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.databinding.FragmentPayBinding
import com.sesameware.smartyard_oem.databinding.FragmentPayWebPageBinding
import com.sesameware.smartyard_oem.ui.main.burger.ExtWebInterface
import com.sesameware.smartyard_oem.ui.main.burger.ExtWebViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class PayAddressFragment : Fragment() {
    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private var _bindingWeb: FragmentPayWebPageBinding? =null
    private val bindingWeb get() = _bindingWeb!!

    private val payViewModel: PayAddressViewModel by sharedStateViewModel()
    private val webViewModel: ExtWebViewModel by sharedViewModel()

    lateinit var adapter: ListDelegationAdapter<List<PayAddressModel>>

    private var isWeb = false

    private fun disableSomeEvents() {
        bindingWeb.wvPayPage.setOnLongClickListener {
            true
        }

        bindingWeb.wvPayPage.setOnDragListener { _, _ ->
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isWeb = DataModule.providerConfig.hasPayments && DataModule.providerConfig.paymentsUrl?.isNotEmpty() == true
        if (isWeb) {
            _bindingWeb = FragmentPayWebPageBinding.inflate(inflater, container, false)
            return bindingWeb.root
        } else {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isWeb) {
            bindingWeb.wvPayPage.clipToOutline = true
            bindingWeb.wvPayPage.settings.javaScriptEnabled = true
            bindingWeb.wvPayPage.settings.javaScriptCanOpenWindowsAutomatically = true
            bindingWeb.wvPayPage.settings.setSupportMultipleWindows(true)

            bindingWeb.wvPayPage.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Timber.d("debug_web console: ${consoleMessage?.message()}")
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    //пытаемся получить URL
                    val href = view?.handler?.obtainMessage()
                    view?.requestFocusNodeHref(href)
                    val url = href?.data?.getString("url")
                    if (url?.isNotEmpty() == true) {
                        //findNavController().popBackStack()
                        val action = PayAddressFragmentDirections.actionGlobalExtWebBottomFragment2(url)
                        findNavController().navigate(action)
                    }
                    return false
                }
            }

            bindingWeb.wvPayPage.addJavascriptInterface(ExtWebInterface(webViewModel,
                object : ExtWebInterface.Callback {
                    override fun onPostLoadingStarted() {
                        bindingWeb.pbPayPage.visibility = View.VISIBLE
                    }

                    override fun onPostLoadingFinished() {
                        bindingWeb.pbPayPage.visibility = View.INVISIBLE
                    }

                }), ExtWebInterface.WEB_INTERFACE_OBJECT)

            bindingWeb.wvPayPage.clearCache(true)
            disableSomeEvents()

            DataModule.providerConfig.paymentsUrl?.let {
                bindingWeb.wvPayPage.loadUrl(it)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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
