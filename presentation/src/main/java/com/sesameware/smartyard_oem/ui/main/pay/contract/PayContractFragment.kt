package com.sesameware.smartyard_oem.ui.main.pay.contract

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
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.smartyard_oem.databinding.FragmentPayContractBinding
import com.sesameware.smartyard_oem.ui.main.pay.PayAddressFragment
import com.sesameware.smartyard_oem.ui.main.pay.PayAddressModel
import com.sesameware.smartyard_oem.ui.main.pay.PayAddressViewModel
import com.sesameware.smartyard_oem.ui.viewPager2.DepthPageTransformer

class PayContractFragment : Fragment() {
    private var _binding: FragmentPayContractBinding? = null
    private val binding get() = _binding!!

    private val payViewModel: PayAddressViewModel by sharedStateViewModel()
    private var payAddressModel: PayAddressModel? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayContractBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        payViewModel.selectedItemIndex.observe(
            viewLifecycleOwner
        ) {
            payAddressModel = payViewModel.addressList.value?.peekContent()!![it ?: 0]
            binding.tvAddress.text = payAddressModel?.address
            setupPager()
        }
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
    }

    private fun setupPager() {
        payAddressModel?.let {
            binding.pageIndicatorView.count = it.accounts.size
            binding.contractViewPager.apply {
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
            binding.contractViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.pageIndicatorView.selection = position
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
