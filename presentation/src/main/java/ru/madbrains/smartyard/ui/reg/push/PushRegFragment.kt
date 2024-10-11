package ru.madbrains.smartyard.ui.reg.sms


import android.app.ActivityManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentPushRegBinding
import ru.madbrains.smartyard.p8
import ru.madbrains.smartyard.ui.main.pay.PayAddressFragment
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment.Companion.BROADCAST_CONFIRM_CODE
import timber.log.Timber


class PushRegFragment : Fragment() {
    private var _binding: FragmentPushRegBinding? = null
    private val binding get() = _binding!!
    private var phoneNumber: String = ""
    private var requestId: String = ""
    private val mViewModel by viewModel<PushRegViewModel>()
    private var name = ""
    private var patronymic = ""
    private var accessToken = ""
    private var isStop = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPushRegBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@PushRegFragment.findNavController()
                        .navigate(R.id.action_pushRegFragment_to_numberRegFragment)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireNotNull(arguments).run {
            phoneNumber = requireNotNull(getString(KEY_PHONE_NUMBER))
            requestId = requireNotNull(getString(REQUEST_ID))
        }
        binding.tvTel.text = String.format(
            getString(R.string.reg_push_code_tel), phoneNumber
        )
        binding.tvCorrectNumberTel.setOnClickListener {
            this.findNavController().navigate(R.id.action_pushRegFragment_to_numberRegFragment)
        }
    }


    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                BROADCAST_CONFIRM_CODE -> {
                    name = intent.getStringExtra("name").toString()
                    patronymic = intent.getStringExtra("patronymic").toString()
                    accessToken = intent.getStringExtra("accessToken").toString()
                    mViewModel.confirmCode(name, patronymic, accessToken, this@PushRegFragment)
                }
                BROADCAST_REJECT_CODE -> {
                    this@PushRegFragment.findNavController()
                        .navigate(R.id.action_pushRegFragment_to_numberRegFragment)
                }
                INTENT_AUTHORIZATION_FAILED -> {
                    this@PushRegFragment.findNavController()
                        .navigate(R.id.action_pushRegFragment_to_smsRegFragment,
                            bundleOf(SmsRegFragment.KEY_PHONE_NUMBER to phoneNumber))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestId.isNotEmpty() && isStop) {
            mViewModel.confirmCodePush(phoneNumber, requestId, this)
        }
    }


    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(BROADCAST_CONFIRM_CODE)
        intentFilter.addAction(BROADCAST_REJECT_CODE)
        intentFilter.addAction(INTENT_AUTHORIZATION_FAILED)
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                intentFilter
            )
        }
    }


    override fun onStop() {
        super.onStop()
        isStop = true
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }


    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
        const val REQUEST_ID = "request_id"
        const val KEY_NAME = "name"
        const val KEY_PATRONYMIC = "patronymic"
        const val BROADCAST_CONFIRM_CODE = "BROADCAST_CONFIRM_CODE"
        const val BROADCAST_REJECT_CODE = "BROADCAST_REJECT_CODE"
        const val INTENT_AUTHORIZATION_FAILED = "INTENT_AUTHORIZATION_FAILED"
    }
}
