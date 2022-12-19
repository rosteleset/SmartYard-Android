package com.sesameware.smartyard_oem.ui.reg.outgoing_call

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentOutgoingCallBinding
import com.sesameware.smartyard_oem.ui.reg.sms.SmsRegFragment

class OutgoingCallFragment : Fragment() {
    private var _binding: FragmentOutgoingCallBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<OutgoingCallViewModel>()
    private var phoneNumber: String = ""
    private var callNumber: String = ""
    private var jobCheckPhone: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    jobCheckPhone?.cancel()
                    this@OutgoingCallFragment.findNavController().navigate(R.id.action_outgoingCallFragment_to_numberRegFragment)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOutgoingCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireNotNull(arguments).run {
            phoneNumber = requireNotNull(getString(KEY_PHONE_NUMBER))
            callNumber = requireNotNull(getString(KEY_CALL_NUMBER))
        }
        binding.tvCaption1OC.text = getString(R.string.outgoing_call_caption_1, phoneNumber)
        binding.tvCaptionNumberOC.text = callNumber

        binding.tvChangeNumberOC.setOnClickListener {
            jobCheckPhone?.cancel()
            findNavController().navigate(R.id.action_outgoingCallFragment_to_numberRegFragment)
        }

        binding.btnMakeCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$callNumber")
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }

        jobCheckPhone = mViewModel.startRepeatingCheckPhone(phoneNumber)

        mViewModel.phoneConfirmed.observe(viewLifecycleOwner) {
            if (it.first) {
                jobCheckPhone?.cancel()
                findNavController().navigate(R.id.action_outgoingCallFragment_to_appealFragment,
                    bundleOf(SmsRegFragment.KEY_NAME to it.second.name,
                        SmsRegFragment.KEY_PATRONYMIC to it.second.patronymic)
                )
            }
        }
    }

    override fun onDestroy() {
        jobCheckPhone?.cancel()
        super.onDestroy()
    }

    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_CALL_NUMBER = "call_number"
    }
}
