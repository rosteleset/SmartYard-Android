package ru.madbrains.smartyard.ui.main.pay.contract

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_pay_contract.*
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.pay.PayAddressFragment
import ru.madbrains.smartyard.ui.main.pay.PayAddressModel
import ru.madbrains.smartyard.ui.main.pay.PayAddressViewModel
import ru.madbrains.smartyard.ui.viewPager2.DepthPageTransformer
import ru.madbrains.smartyard.utils.stateSharedViewModel

class PayContractFragment : Fragment() {

    private val payViewModel: PayAddressViewModel by stateSharedViewModel()
    var payAddressModel: PayAddressModel? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pay_contract, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        payViewModel.selectedItemIndex.observe(
            viewLifecycleOwner,
            Observer {
                payAddressModel = payViewModel.addressList.value?.peekContent()!![it ?: 0]
                tvAddress.text = payAddressModel?.address
                setupPager()
            }
        )
        ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
    }

    private fun setupPager() {
        payAddressModel?.let {
            pageIndicatorView.count = it.accounts.size
            contractViewPager.apply {
                setPageTransformer(DepthPageTransformer())
                adapter = PayContractPagerAdapter(requireActivity(), it.accounts) {
                    with(it) {
                        val action =
                            PayContractFragmentDirections.actionPayContractFragmentToPayBottomSheetDialogFragment(
                                contractPayName,
                                payAdvice,
                                lcabPay,
                                clientId,
                                contractName
                            )
                        this@PayContractFragment.findNavController()
                            .navigate(action)
                    }
                }
                (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            }
            contractViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    pageIndicatorView.selection = position
                }
            })
        }
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
                    PayAddressFragment.BROADCAST_PAY_UPDATE
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
}
